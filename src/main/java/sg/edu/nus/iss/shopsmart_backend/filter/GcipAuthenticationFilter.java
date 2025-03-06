package sg.edu.nus.iss.shopsmart_backend.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.model.GcipAccLookupResponse;
import sg.edu.nus.iss.shopsmart_backend.model.GcipRefreshTokenResponse;
import sg.edu.nus.iss.shopsmart_backend.service.AuthService;
import org.springframework.security.core.context.SecurityContextHolder;
import sg.edu.nus.iss.shopsmart_backend.service.CommonService;
import sg.edu.nus.iss.shopsmart_backend.utils.RedisManager;

import java.io.IOException;
import java.util.Collections;

import static sg.edu.nus.iss.shopsmart_backend.utils.ApplicationConstants.*;
import static sg.edu.nus.iss.shopsmart_backend.utils.RedisKeys.*;

public class GcipAuthenticationFilter extends OncePerRequestFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(GcipAuthenticationFilter.class);
    private final ObjectMapper mapper = Json.mapper();

    private final AuthService authService;
    private final CommonService commonService;
    private final RedisManager redisManager;

    @Autowired
    public GcipAuthenticationFilter(AuthService authService, CommonService commonService, RedisManager redisManager) {
        this.authService = authService;
        this.commonService = commonService;
        this.redisManager = redisManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        // Skip filter for specific endpoints
        if (path.startsWith("/auth/google/") || path.startsWith("/auth/native/")
                || path.startsWith("/profile/") || path.startsWith("/redis-api/")
                || path.matches("/") || path.matches("/home")) {
            filterChain.doFilter(request, response);
            return;
        }

        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, "user-val", null);
//        String sessionId = extractSessionIdFromCookies(request.getCookies());
        String sessionId = apiRequestResolver.getSessionId();
        if (sessionId != null) {
            String gcipIdToken = redisManager.getHashValue(REDIS_SESSION_PREFIX.concat(sessionId), GCIP_ID_TOKEN);
            if (gcipIdToken != null && !gcipIdToken.isEmpty()) {
                try {
                    GcipAccLookupResponse gcipAccLookupResponse = authService.lookupExistingUserThroughGcip(gcipIdToken, sessionId, apiRequestResolver.getLoggerString());
                    if(gcipAccLookupResponse.isIdTokenRefreshNeeded()){
                        String updatedIdToken = refreshIdTokenAndReturnNewOne(sessionId, apiRequestResolver.getLoggerString());
                        if(updatedIdToken==null || updatedIdToken.isEmpty()){
                            log.info("GCIP verification failed due to unable to refresh id_token for sessionId: {}, so unable to verify user", sessionId);
                            apiRequestResolver.setUserId(null);
                            apiRequestResolver.setLoggedIn(false);
                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                    "", null, Collections.emptyList());
                            auth.setDetails(apiRequestResolver);
                            SecurityContextHolder.getContext().setAuthentication(auth);
                            filterChain.doFilter(request, response);
                            return;
                        }
                        gcipAccLookupResponse = authService.lookupExistingUserThroughGcip(updatedIdToken, sessionId, apiRequestResolver.getLoggerString());
                        if(gcipAccLookupResponse.isIdTokenRefreshNeeded()){
                            log.info("{} as got error a second time while refreshing token, so issue with either refreshToken or unable to verify user", apiRequestResolver.getLoggerString());
                            apiRequestResolver.setUserId(null);
                            apiRequestResolver.setLoggedIn(false);
                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                    "", null, Collections.emptyList());
                            auth.setDetails(apiRequestResolver);
                            SecurityContextHolder.getContext().setAuthentication(auth);
                            filterChain.doFilter(request, response);
                            return;
                        }
                    }

                    String userId = fetchValidatedUser(gcipAccLookupResponse, sessionId);
                    if (userId != null && !userId.isEmpty()) {
                        log.info("GCIP verification successful for sessionId: {} with userId: {}", sessionId, userId);
                        apiRequestResolver.setUserId(userId);
                        apiRequestResolver.setLoggedIn(true);
                        commonService.updateUserIdInRedisInSessionData(apiRequestResolver);
                        //TODO :: based on loginType consider using "OAuth2AuthenticationToken" class for Authentication object. Explore the same.
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                userId, null, Collections.emptyList());
                        auth.setDetails(apiRequestResolver);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } else {
                        log.info("GCIP verification failed for sessionId: {} unable to verify user", sessionId);
                        apiRequestResolver.setUserId(null);
                        apiRequestResolver.setLoggedIn(false);
                        //based on API config in redis then if protected then should return error resp
                        //to be properly handled in ApiService/ApiController.
                        //return here a basic Authentication object with empty userId and email.
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                "", null, Collections.emptyList());
                        auth.setDetails(apiRequestResolver);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                } catch (Exception ex) {
                    logger.error("Some exception caused GCIP verification to fail, here, we will block further processing of request.", ex);
                    apiRequestResolver.setUserId(null);
                    apiRequestResolver.setLoggedIn(false);
                    //based on API config in redis then if protected then should return error resp
                    //to be properly handled in ApiService/ApiController.
                    //return here a basic Authentication object with empty userId and email.
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("", null);
                    auth.setDetails(apiRequestResolver);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } else {
                log.info("GCIP ID Token is null or empty for sessionId: {} unable to verify user", sessionId);
                apiRequestResolver.setUserId(null);
                apiRequestResolver.setLoggedIn(false);
                //based on API config in redis then if protected then should return error resp
                //to be properly handled in ApiService/ApiController.
                //return here a basic Authentication object with empty userId and email.
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        "", null, Collections.emptyList());
                auth.setDetails(apiRequestResolver);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        // Continue filter chain
        filterChain.doFilter(request, response);
    }

    private String extractSessionIdFromCookies(Cookie[] cookies) {
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SESSION_ID.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private String fetchValidatedUser(GcipAccLookupResponse gcipAccLookupResp, String sessionId) {
        String emailFromGcip = gcipAccLookupResp.getUsers().getFirst().getProviderUserInfo().getFirst().getEmail();
        String emailInSession = redisManager.getHashValue(REDIS_SESSION_PREFIX.concat(sessionId), EMAIL);
        if(emailFromGcip==null || emailFromGcip.isEmpty()){
            log.info("GCIP response does not contain email for sessionId: {}", sessionId);
            return null;
        }
        if(emailInSession==null || emailInSession.isEmpty()){
            log.info("Session {} does not have logged in user, no need to identify user", sessionId);
            return "";
        }
        if(emailInSession.equals(emailFromGcip)){
            log.info("GCIP response email matches with session email for sessionId: {}, hence need to fetch userId for the same.", sessionId);
            //for now we are not fetching userId from profile, we will directly take it from session itself.
            //TODO :: handle userId fetch from profile service instead of directly from session.
            String userIdFromSession = redisManager.getHashValue(REDIS_SESSION_PREFIX.concat(sessionId), USER_ID);
            log.info("As GCIP response email matches with session email for sessionId: {}, hence returning userId: {}", sessionId, userIdFromSession);
            return userIdFromSession;
        } else{
            log.info("GCIP response email does not match with session email for sessionId: {}, user not validated.", sessionId);
            return "";
        }
    }

    private String refreshIdTokenAndReturnNewOne(String sessionId, String loggerString){
        String refreshToken = redisManager.getHashValue(REDIS_SESSION_PREFIX.concat(sessionId), GCIP_REFRESH_TOKEN);
        if(refreshToken==null || refreshToken.isEmpty()){
            log.info("{} Refresh token is null or empty for sessionId: {} unable to refresh token, user needs to signin again", loggerString, sessionId);
            return null;
        }
        try {
            GcipRefreshTokenResponse gcipRefreshTokenResponse = authService.refreshIdTokenThroughGcip(refreshToken, sessionId, loggerString);
            if(gcipRefreshTokenResponse==null || gcipRefreshTokenResponse.getIdToken()==null || gcipRefreshTokenResponse.getIdToken().isEmpty()){
                log.info("{} Refreshing token call failed for sessionId: {} unable to refresh token. User needs to sign in again.", loggerString, sessionId);
                return null;
            }
            if(gcipRefreshTokenResponse.isInvalidRefreshToken()){
                log.info("{} Refresh token is invalid for sessionId: {} unable to refresh id_token. User needs to sign in again.", loggerString, sessionId);
                return null;
            }
            String updatedIdToken = gcipRefreshTokenResponse.getIdToken();
            authService.updateSessionInRedis(sessionId, GCIP_ID_TOKEN, updatedIdToken, loggerString);
            log.info("{} Updated ID Token post refresh_token use for sessionId: {} with new token: {}", loggerString, sessionId, updatedIdToken);
            return updatedIdToken;
        } catch (Exception ex) {
            logger.error("Some exception caused GCIP id_token refresh to fail, we will block further processing of request. User needs to sign in again", ex);
            return null;
        }
    }

}

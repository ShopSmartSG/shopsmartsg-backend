package sg.edu.nus.iss.shopsmart_backend.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.model.GcipAccLookupResponse;
import sg.edu.nus.iss.shopsmart_backend.model.GcipRefreshTokenResponse;
import sg.edu.nus.iss.shopsmart_backend.service.AuthService;
import org.springframework.security.core.context.SecurityContextHolder;
import sg.edu.nus.iss.shopsmart_backend.service.CommonService;
import sg.edu.nus.iss.shopsmart_backend.utils.RedisManager;
import sg.edu.nus.iss.shopsmart_backend.utils.Utils;

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
        if (path.startsWith("/auth/google/") || path.startsWith("/auth/native/")
                || path.startsWith("/profile/") || path.startsWith("/redis-api/")
                || path.matches("/") || path.matches("/home")) {
            log.info("Path {} is allowed path, no need to verify user", path);
            filterChain.doFilter(request, response);
            return;
        }
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            log.info("Request method is OPTIONS, no need to verify user");
            filterChain.doFilter(request, response);
            return;
        }

        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, "user-val", null);
        String sessionId = apiRequestResolver.getSessionId();
        if (sessionId != null) {
            String gcipIdToken = redisManager.getHashValue(REDIS_SESSION_PREFIX.concat(sessionId), GCIP_ID_TOKEN);
            if (gcipIdToken != null && !gcipIdToken.isEmpty()) {
                try {
                    GcipAccLookupResponse gcipAccLookupResponse = authService.lookupExistingUserThroughGcip(gcipIdToken, sessionId, apiRequestResolver.getLoggerString());
                    if(gcipAccLookupResponse.isIdTokenRefreshNeeded()){
                        String updatedIdToken = refreshIdTokenAndReturnNewOne(sessionId, apiRequestResolver.getLoggerString());
                        if(updatedIdToken==null || updatedIdToken.isEmpty()){
                            log.info("{} GCIP verification failed for session due to unable to refresh id_token, so unable to verify user", apiRequestResolver.getLoggerString());
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

                    String userId = fetchValidatedUser(gcipAccLookupResponse, sessionId, apiRequestResolver.getLoggerString());
                    if (userId != null && !userId.isEmpty()) {
                        log.info("{} GCIP verification successful for session with userId: {}", apiRequestResolver.getLoggerString(), userId);
                        apiRequestResolver.setUserId(userId);
                        apiRequestResolver.setLoggedIn(true);
                        commonService.updateUserIdInRedisInSessionData(apiRequestResolver);
                        //TODO :: based on loginType consider using "OAuth2AuthenticationToken" class for Authentication object. Explore the same.
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                userId, null, Collections.emptyList());
                        auth.setDetails(apiRequestResolver);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } else {
                        log.info("{} GCIP verification failed for session unable to verify user", apiRequestResolver.getLoggerString());
                        apiRequestResolver.setUserId(null);
                        apiRequestResolver.setLoggedIn(false);
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                "", null, Collections.emptyList());
                        auth.setDetails(apiRequestResolver);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                } catch (Exception ex) {
                    logger.error("Some exception caused GCIP verification to fail, here, we will block further processing of request.", ex);
                    apiRequestResolver.setUserId(null);
                    apiRequestResolver.setLoggedIn(false);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("", null);
                    auth.setDetails(apiRequestResolver);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } else {
                log.info("{} GCIP ID Token is null or empty for session, unable to verify user", apiRequestResolver.getLoggerString());
                apiRequestResolver.setUserId(null);
                apiRequestResolver.setLoggedIn(false);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        "", null, Collections.emptyList());
                auth.setDetails(apiRequestResolver);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String fetchValidatedUser(GcipAccLookupResponse gcipAccLookupResp, String sessionId, String loggerString) {
        String emailFromGcip = gcipAccLookupResp.getUsers().getFirst().getProviderUserInfo().getFirst().getEmail();
        String emailInSession = redisManager.getHashValue(REDIS_SESSION_PREFIX.concat(sessionId), EMAIL);
        String loginTypeInSession = redisManager.getHashValue(REDIS_SESSION_PREFIX.concat(sessionId), LOGIN_TYPE);
        String profileType = redisManager.getHashValue(REDIS_SESSION_PREFIX.concat(sessionId), PROFILE_TYPE);
        log.debug("{} fetched loginType: {} and profileType: {} from session", loggerString, loginTypeInSession, profileType);
        if(emailFromGcip==null || emailFromGcip.isEmpty()){
            log.info("{} GCIP response does not contain email for session", loggerString);
            return null;
        }
        if(emailInSession==null || emailInSession.isEmpty()){
            log.info("{} Session does not have logged in user, no need to identify user", loggerString);
            return "";
        }
        if(emailInSession.equals(emailFromGcip)){
            log.info("{} GCIP response email matches with session email, hence need to fetch userId for the same.", loggerString);
            //for now we are not fetching userId from profile, we will directly take it from session itself.
            //TODO :: handle userId fetch from profile service instead of directly from session.
            String userIdFromSession = redisManager.getHashValue(REDIS_SESSION_PREFIX.concat(sessionId), USER_ID);
            log.info("{} As GCIP response email matches with session email, hence returning userId: {}", loggerString, userIdFromSession);
            return userIdFromSession;
        } else if(loginTypeInSession.equals(NATIVE)){
            log.info("{} as login type is NATIVE, hence need to check if profileType based email matches gcip email or not.", loggerString);
            String profileTypeBasedEmailForNativeLogin = Utils.insertProfileTypeIntoEmail(emailInSession, profileType);
            log.debug("{} profileTypeBasedEmailForNativeLogin: {}", loggerString, profileTypeBasedEmailForNativeLogin);
            if(profileTypeBasedEmailForNativeLogin.equals(emailFromGcip)){
                //for now we are not fetching userId from profile, we will directly take it from session itself.
                //TODO :: handle userId fetch from profile service instead of directly from session.
                String userIdFromSession = redisManager.getHashValue(REDIS_SESSION_PREFIX.concat(sessionId), USER_ID);
                log.info("{} As login type is NATIVE and email from GCIP matches with profileTypeBasedEmailForNativeLogin, hence returning userId: {}", loggerString, userIdFromSession);
                return userIdFromSession;
            }else{
                log.info("{} GCIP response email does not match with profileType based session email for native login either, user not validated.", loggerString);
                return "";
            }
        } else{
            log.info("{} GCIP response email does not match with session email for session, user not validated.", loggerString);
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

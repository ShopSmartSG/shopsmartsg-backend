package sg.edu.nus.iss.shopsmart_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.utils.Constants;
import sg.edu.nus.iss.shopsmart_backend.utils.RedisManager;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CommonService extends Constants {
    private static final Logger log = LoggerFactory.getLogger(CommonService.class);

    private final RedisManager redisManager;

    @Autowired
    public CommonService(RedisManager redisManager) {
        this.redisManager = redisManager;
    }

    public ApiRequestResolver createApiResolverRequest(HttpServletRequest request, String apiKey, JsonNode requestBody) {
        log.info("Creating API request resolver for API key: {}", apiKey);
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setCorrelationId(UUID.randomUUID().toString());
        apiRequestResolver.setApiKey(apiKey);
        apiRequestResolver.setRequestBody(requestBody);
        apiRequestResolver.setIpAddress(request.getRemoteAddr());
        apiRequestResolver.setRequestUri(request.getRequestURI());

        // Extract headers
        log.info("{} Extracting headers from request {}", apiRequestResolver.getCorrelationId(), request.getHeaderNames());
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        apiRequestResolver.setHeaders(headers);

        // Extract query parameters
        log.info("{} Extracting query parameters from request {}", apiRequestResolver.getCorrelationId(), request.getParameterMap());
        Map<String, String> queryParams = new HashMap<>();
        request.getParameterMap().forEach((key, value) -> queryParams.put(key, value[0]));
        apiRequestResolver.setQueryParams(queryParams);

        // Extract cookies
        log.info("{} Extracting cookies from request {}", apiRequestResolver.getCorrelationId(), request.getCookies());
        Map<String, String> cookies = new HashMap<>();
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                cookies.put(cookie.getName(), cookie.getValue());
            }
        }
        apiRequestResolver.setCookies(cookies);
        log.debug("{} Cookies: {}", apiRequestResolver.getCorrelationId(), apiRequestResolver.getCookies());

        // Extract session information
        log.info("{} Extracting or setting session information from request", apiRequestResolver.getCorrelationId());
        String sessionId= "";
        log.debug("{} getting jsessionId from cookies", apiRequestResolver.getCorrelationId());
        if(cookies.containsKey(SESSION_ID) && StringUtils.isNotEmpty(cookies.get(SESSION_ID))){
            sessionId = cookies.get(SESSION_ID);
            checkAndUpdateSessionData(apiRequestResolver, sessionId, getExistingSessionData(cookies.get(SESSION_ID)));
        } else {
            sessionId = UUID.randomUUID().toString();
            apiRequestResolver.setSessionId(sessionId);
            apiRequestResolver.setSessionAttributes(createNewSessionAndStore(sessionId));// Store session ID in Redis with 30 minutes validity
        }

        apiRequestResolver.setLoggerString(createLoggerString(apiRequestResolver));

        String requestUri = request.getRequestURI();
        String additionalUriData = "";
        if(requestUri.contains(apiKey.concat(SLASH))){
            additionalUriData = request.getRequestURI().split(apiKey + "/")[1];
            apiRequestResolver.setAdditionalUriData(additionalUriData);
            log.info("{} For the api key {} found additional data to be provided : {}", apiRequestResolver.getLoggerString(),
                    apiRequestResolver.getApiKey(), apiRequestResolver.getAdditionalUriData());
        }
        log.info("Completed creating API request resolver for API key: {}, as {}", apiKey, apiRequestResolver);
        return apiRequestResolver;
    }

    private boolean checkIfUserIsLoggedIn(String userId, Map<String, String> sessionData){
        return sessionData.containsKey(IS_LOGGED_IN) && TRUE.equalsIgnoreCase(sessionData.get(IS_LOGGED_IN))
                && sessionData.containsKey(USER_ID) && StringUtils.isNotEmpty(userId) && userId.equals(sessionData.get(USER_ID));
    }

    private void checkAndUpdateSessionData(ApiRequestResolver apiRequestResolver,String sessionId, Map<String, String> sessionData){
        if(sessionData==null || sessionData.isEmpty()){
            log.info("No session data found for session id: {}, will set new session in redis", sessionId);
            sessionId = UUID.randomUUID().toString();
            apiRequestResolver.setSessionId(sessionId);
            apiRequestResolver.setSessionAttributes(createNewSessionAndStore(sessionId));
            apiRequestResolver.setLoggedIn(false);
        } else {
            apiRequestResolver.setSessionId(sessionId);
            if(checkIfUserIsLoggedIn(apiRequestResolver.getCookies().get(USER_ID), sessionData)){
                log.info("Session {} is logged in for with userId {} in session data matching with cookie val {}, so extending the session.",
                        sessionId, sessionData.get(USER_ID), apiRequestResolver.getCookies().get(USER_ID));
                sessionData.put(VALID_TILL, String.valueOf(System.currentTimeMillis() + 30 * 60 * 1000));
                redisManager.setHashMap(REDIS_SESSION_PREFIX.concat(sessionId), sessionData);
                apiRequestResolver.setSessionAttributes(sessionData);
                apiRequestResolver.setUserId(sessionData.get(USER_ID));
                apiRequestResolver.setLoggedIn(true);
            } else {
                log.info("Session {} is not logged in, will check if session change is needed or not", sessionId);
                if(System.currentTimeMillis() > Long.parseLong(sessionData.get(VALID_TILL))){
                    redisManager.deleteKey(REDIS_SESSION_PREFIX.concat(sessionId));
                    log.info("Session is expired, will create new session");
                    sessionId = UUID.randomUUID().toString();
                    apiRequestResolver.setSessionId(sessionId);
                    apiRequestResolver.setSessionAttributes(createNewSessionAndStore(sessionId));
                    apiRequestResolver.setLoggedIn(false);
                } else {
                    log.info("Session {} is not expired, will update the session data", sessionId);
                    sessionData.put(VALID_TILL, String.valueOf(System.currentTimeMillis() + 30 * 60 * 1000));
                    //also we need to remove userId from session data if present
                    if(sessionData.containsKey(USER_ID) && StringUtils.isNotEmpty(sessionData.get(USER_ID))){
                        sessionData.remove(USER_ID);
                        apiRequestResolver.setLoggedIn(false);
                    }
                    redisManager.setHashMap(REDIS_SESSION_PREFIX.concat(sessionId), sessionData);
                    apiRequestResolver.setSessionAttributes(sessionData);
                }
            }
        }
    }

    private Map<String, String> createNewSessionAndStore(String sessionId){
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put(SESSION_ID, sessionId);
        sessionData.put(IS_LOGGED_IN, "false");
        sessionData.put(VALID_TILL, String.valueOf(System.currentTimeMillis() + 30 * 60 * 1000));
        redisManager.setHashMap(REDIS_SESSION_PREFIX.concat(sessionId), sessionData);
        return sessionData;
    }

    private Map<String, String> getExistingSessionData(String sessionId){
        return redisManager.getHashMap(REDIS_SESSION_PREFIX.concat(sessionId));
    }

    private String createLoggerString(ApiRequestResolver apiRequestResolver){
        return CORRELATION_ID.concat(" :: ").concat(apiRequestResolver.getCorrelationId()).concat(COMMA)
                .concat(EMPTY_SPACE).concat(SESSION_ID).concat(" :: ").concat(apiRequestResolver.getSessionId());
    }

    public void updateUserIdInRedisInSessionData(ApiRequestResolver apiRequestResolver){
        log.info("{} starting to store userId {} in session data in redis", apiRequestResolver.getLoggerString(), apiRequestResolver.getUserId());
        redisManager.setHashValue(REDIS_SESSION_PREFIX.concat(apiRequestResolver.getSessionId()), USER_ID, apiRequestResolver.getUserId());
        redisManager.setHashValue(REDIS_SESSION_PREFIX.concat(apiRequestResolver.getSessionId()), IS_LOGGED_IN, TRUE);
    }
}

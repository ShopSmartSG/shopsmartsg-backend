package sg.edu.nus.iss.shopsmart_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.redis.RedisManager;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Service
public class CommonService {
    private static final Logger log = LoggerFactory.getLogger(CommonService.class);

    private final RedisManager redisManager;

    @Autowired
    public CommonService(RedisManager redisManager) {
        this.redisManager = redisManager;
    }

    public ApiRequestResolver createApiResolverRequest(HttpServletRequest request, String apiKey, JsonNode requestBody) {
        log.info("Creating API request resolver for API key: {}", apiKey);
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setApiKey(apiKey);
        apiRequestResolver.setRequestBody(requestBody);
        apiRequestResolver.setIpAddress(request.getRemoteAddr());

        // Extract headers
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        apiRequestResolver.setHeaders(headers);

        // Extract query parameters
        Map<String, String> queryParams = new HashMap<>();
        request.getParameterMap().forEach((key, value) -> queryParams.put(key, value[0]));
        apiRequestResolver.setQueryParams(queryParams);

        // Extract cookies
        Map<String, String> cookies = new HashMap<>();
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                cookies.put(cookie.getName(), cookie.getValue());
            }
        }
        apiRequestResolver.setCookies(cookies);

        // Extract session information
        HttpSession session = request.getSession(false);
        if (session != null) {
            apiRequestResolver.setSessionId(session.getId());
        }
        log.info("Completed creating API request resolver for API key: {}, as {} ", apiKey, apiRequestResolver);
        return apiRequestResolver;
    }
}

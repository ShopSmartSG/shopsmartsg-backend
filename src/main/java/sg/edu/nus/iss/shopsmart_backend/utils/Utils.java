package sg.edu.nus.iss.shopsmart_backend.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;

@Service
public class Utils extends Constants{
    private static final Logger log = LoggerFactory.getLogger(Utils.class);
    public static HttpMethod getHttpMethod(String method){
        switch (method) {
            case "POST":
                return HttpMethod.POST;
            case "PUT":
                return HttpMethod.PUT;
            case "DELETE":
                return HttpMethod.DELETE;
            case "PATCH":
                return HttpMethod.PATCH;
            default:
                return HttpMethod.GET;
        }
    }

    public static String getDdoForFetchProfileIdByType(String profileType){
        switch (profileType) {
            case CUSTOMER:
                return FETCH_CUSTOMER_ID_BY_EMAIL;
            case MERCHANT:
                return FETCH_MERCHANT_ID_BY_EMAIL;
            case DELIVERY:
                return FETCH_DELIVERY_PARTNER_ID_BY_EMAIL;
            default:
                return null;
        }
    }

    public static String ddoCreateProfileByType(String profileType){
        switch (profileType) {
            case CUSTOMER:
                return CREATE_CUSTOMER_PROFILE;
            case MERCHANT:
                return CREATE_MERCHANT_PROFILE;
            case DELIVERY:
                return CREATE_DELIVERY_PARTNER_PROFILE;
            default:
                return null;
        }
    }

    public void setSessionAndCookieDataForSession(ApiRequestResolver apiRequestResolver, HttpServletRequest request,
                                                   HttpServletResponse response) {
        log.info("Setting session id {} in cookies and session attributes", apiRequestResolver.getSessionId());
        String sessionId = apiRequestResolver.getSessionId();
        String domain = request.getServerName();
        // Set session ID in response cookies
        Cookie sessionCookie = new Cookie(SESSION_ID, sessionId);
        sessionCookie.setPath("/");
        sessionCookie.setHttpOnly(true);
        sessionCookie.setDomain(domain);
        sessionCookie.setAttribute("SameSite", "Lax");
        sessionCookie.setMaxAge(60 * 30); // 30 minutes
        response.addCookie(sessionCookie);

        log.debug("printing cookies for response for sessionId : {}", response.getHeaders("Set-Cookie"));
    }

    public void setUserIdCookieNeededOrRemove(ApiRequestResolver apiRequestResolver, HttpServletRequest request,
                                              HttpServletResponse response){
        log.debug("starting setting of userid in cookies for {}", apiRequestResolver);
        String domain = request.getServerName();
        Cookie userIdCookie;
        String existingUserId = "";
        if(apiRequestResolver.getCookies().containsKey(USER_ID) && apiRequestResolver.getCookies().get(USER_ID)!=null
                && StringUtils.isNotEmpty(apiRequestResolver.getCookies().get(USER_ID))){
            existingUserId = apiRequestResolver.getCookies().get(USER_ID);
        }
        if (StringUtils.isNotEmpty(existingUserId) &&
                (apiRequestResolver.getUserId() == null || StringUtils.isEmpty(apiRequestResolver.getUserId()) || !apiRequestResolver.isLoggedIn())) {
            log.info("Removing user id cookie with value {}", existingUserId);
            userIdCookie = new Cookie(USER_ID, null);
            userIdCookie.setPath(SLASH);
            userIdCookie.setHttpOnly(true);
            userIdCookie.setDomain(domain);
            userIdCookie.setAttribute("SameSite", "Lax");
            userIdCookie.setMaxAge(0); // This will delete the cookie
            response.addCookie(userIdCookie);

            log.debug("printing cookies for response for removing userId : {}", response.getHeaders("Set-Cookie"));
            return;
        }
        if(apiRequestResolver.getUserId() != null && StringUtils.isNotEmpty(apiRequestResolver.getUserId()) && apiRequestResolver.isLoggedIn()){
            log.info("Setting user id {} in cookies as user is now logged in", apiRequestResolver.getUserId());
            userIdCookie = new Cookie(USER_ID, apiRequestResolver.getUserId());
            userIdCookie.setPath(SLASH);
            userIdCookie.setHttpOnly(true);
            userIdCookie.setDomain(domain);
            userIdCookie.setAttribute("SameSite", "Lax");
            userIdCookie.setMaxAge(60 * 60 * 24 * 30); // 30 days
            response.addCookie(userIdCookie);

            log.debug("printing cookies for response for adding userId : {}", response.getHeaders("Set-Cookie"));
            return;
        }
    }

    public static HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range");
        headers.add("Access-Control-Expose-Headers", "Content-Length,Content-Range");
        return headers;
    }
}

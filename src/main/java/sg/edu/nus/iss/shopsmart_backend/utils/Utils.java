package sg.edu.nus.iss.shopsmart_backend.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
        return switch (method) {
            case "POST" -> HttpMethod.POST;
            case "PUT" -> HttpMethod.PUT;
            case "DELETE" -> HttpMethod.DELETE;
            case "PATCH" -> HttpMethod.PATCH;
            default -> HttpMethod.GET;
        };
    }

    public static String getDdoForFetchProfileIdByType(String profileType){
        return switch (profileType) {
            case CUSTOMER -> FETCH_CUSTOMER_ID_BY_EMAIL;
            case MERCHANT -> FETCH_MERCHANT_ID_BY_EMAIL;
            case DELIVERY -> FETCH_DELIVERY_ID_BY_EMAIL;
            default -> null;
        };
    }

    public static String ddoCreateProfileByType(String profileType){
        return switch (profileType) {
            case CUSTOMER -> CREATE_CUSTOMER_PROFILE;
            case MERCHANT -> CREATE_MERCHANT_PROFILE;
            case DELIVERY -> CREATE_DELIVERY_PROFILE;
            default -> null;
        };
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
        sessionCookie.setMaxAge(60 * 30); // 30 minutes
        response.addCookie(sessionCookie);

        response.addHeader("Set-Cookie", SESSION_ID.concat(EQUALS).concat(sessionId)
                .concat("; Path=/; HttpOnly; Max-Age=").concat(String.valueOf(60 * 30))
                .concat("; Domain=").concat(domain)
                .concat("; SameSite=Lax"));
//        response.setHeader("Set-Cookie", SESSION_ID.concat(EQUALS) + sessionId + "; Path=/; HttpOnly");
        //handle session here.
//        HttpSession session = request.getSession(false);
//
//        if (session == null || !sessionId.equals(session.getId())) {
//            session = request.getSession(true);
//            session.setAttribute(SESSION_ID, sessionId);
//        }
        // Set additional session attributes
//        apiRequestResolver.getSessionAttributes().forEach(session::setAttribute);
    }

    public void setUserIdCookieNeededOrRemove(ApiRequestResolver apiRequestResolver, HttpServletRequest request,
                                              HttpServletResponse response){
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
            userIdCookie.setMaxAge(0); // This will delete the cookie
            response.addCookie(userIdCookie);

            response.addHeader("Set-Cookie", USER_ID.concat(EQUALS)
                    .concat("; Path=/; HttpOnly; Max-Age=0; Domain=").concat(domain)
                    .concat("; SameSite=Lax"));
            return;
        }
        if(apiRequestResolver.getUserId() != null && StringUtils.isNotEmpty(apiRequestResolver.getUserId()) && apiRequestResolver.isLoggedIn()){
            log.info("Setting user id {} in cookies as user is now logged in", apiRequestResolver.getUserId());
            userIdCookie = new Cookie(USER_ID, apiRequestResolver.getUserId());
            userIdCookie.setPath(SLASH);
            userIdCookie.setHttpOnly(true);
            userIdCookie.setDomain(domain);
            userIdCookie.setMaxAge(60 * 60 * 24 * 30); // 30 days
            response.addCookie(userIdCookie);

            response.addHeader("Set-Cookie", USER_ID.concat(EQUALS).concat(apiRequestResolver.getUserId())
                    .concat("; Path=/; HttpOnly; Max-Age=").concat(String.valueOf(60 * 60 * 24 * 30))
                    .concat("; Domain=").concat(domain)
                    .concat("; SameSite=Lax"));
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

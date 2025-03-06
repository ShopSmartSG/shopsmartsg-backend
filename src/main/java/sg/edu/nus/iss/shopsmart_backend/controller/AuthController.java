package sg.edu.nus.iss.shopsmart_backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.model.GcipNativeLoginTokenResp;
import sg.edu.nus.iss.shopsmart_backend.model.GcipSignInWithIdpTokenResponse;
import sg.edu.nus.iss.shopsmart_backend.model.OAuth2TokenExchangeResponse;
import sg.edu.nus.iss.shopsmart_backend.service.CommonService;
import sg.edu.nus.iss.shopsmart_backend.service.AuthService;
import sg.edu.nus.iss.shopsmart_backend.service.ProfileService;
import sg.edu.nus.iss.shopsmart_backend.utils.Constants;
import sg.edu.nus.iss.shopsmart_backend.utils.Utils;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/auth")
@Tag(name = "User Login flows", description = "Handle user login for customers and merchants profiles via APIs. To support both google and native login.")
public class AuthController extends Constants {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final ObjectMapper mapper = Json.mapper();

    @Value("${frontend_url}")
    private String frontendUrl;

    private final AuthService authService;
    private final CommonService commonService;
    private final ProfileService profileService;
    private final Utils utils;

    @Autowired
    public AuthController(AuthService authService, CommonService commonService, ProfileService profileService, Utils utils) {
        this.authService = authService;
        this.commonService = commonService;
        this.profileService = profileService;
        this.utils = utils;
    }

    @GetMapping("/google/login/{profile-type}")
    public ResponseEntity<String> googleLoginUrl(@PathVariable(name = "profile-type") String profileType,
                                                 HttpServletRequest request, HttpServletResponse response){
        log.info("Starting flow for google login for profile type: {}", profileType);
        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, "google-login", null);
        // Generate google login url
        String googleLoginUrl = authService.generateAuthorizationUrl(apiRequestResolver, profileType);
        setRequiredCookies(apiRequestResolver, request, response);
        HttpHeaders headers = Utils.createHeaders(request);
//        headers.setLocation(URI.create(googleLoginUrl));
        return new ResponseEntity<>(googleLoginUrl,headers, HttpStatus.FOUND);
    }

//    @PostMapping("/google/signup/{profile-type}")
//    public ResponseEntity<String> googleSignupUrl(@PathVariable(name = "profile-type") String profileType,
//                                                  @RequestBody JsonNode requestBody, HttpServletRequest request, HttpServletResponse response){
//        log.info("Starting flow for google signup for profile type: {}", profileType);
//        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, "google-signup", requestBody);
//        //additional step to store profile object for signUp temporarily in session.
//        // Generate google signup url
//        String googleSignupUrl = authService.generateAuthorizationUrl(apiRequestResolver, profileType);
//        setRequiredCookies(apiRequestResolver, request, response);
//        return new ResponseEntity<>(googleSignupUrl, Utils.createHeaders(), HttpStatus.FOUND);
//    }

    @GetMapping("/google/callback/{profile-type}")
    public CompletableFuture<ResponseEntity<String>> handleGoogleCallback(@PathVariable(name = "profile-type") String profileType,
                                                  @RequestParam String code, @RequestParam String state,
                                                  HttpServletRequest request, HttpServletResponse response){
        log.info("Starting flow for google callback with code : {} and state: {} for profileType: {}", code, state, profileType);
        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, "google-login", null);
        HttpHeaders headers = Utils.createHeaders(request);
        setRequiredCookies(apiRequestResolver, request, response);
        return authService.performGoogleCallbackHandling(apiRequestResolver, code, state, profileType).thenApplyAsync(callbackHandlingRespURI -> {
            log.info("{} Received response from google callback handling: {}", apiRequestResolver.getLoggerString(), callbackHandlingRespURI);
            headers.setLocation(callbackHandlingRespURI);
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        });
    }

    @GetMapping("/native/signup/{profile-type}")
    public CompletableFuture<ResponseEntity<JsonNode>> nativeSignUp(@PathVariable(name = "profile-type") String profileType, @RequestBody JsonNode requestBody,
                             HttpServletRequest request, HttpServletResponse response){
        log.info("Starting flow for native sign up for profile type: {}", profileType);
        HttpHeaders headers = Utils.createHeaders(request);
        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, "native-signup", requestBody);
        return authService.performNativeSignUp(apiRequestResolver, profileType).thenApplyAsync(apiResponseResolver -> {
            log.info("{} Received response from native sign up flow: {}", apiRequestResolver.getLoggerString(), apiResponseResolver);
            setRequiredCookies(apiRequestResolver, request, response);
            if(!apiResponseResolver.getStatusCode().equals(HttpStatus.FOUND)){
                return new ResponseEntity<>(apiResponseResolver.getRespData(), headers, apiResponseResolver.getStatusCode());
            }else{
                setRequiredCookies(apiRequestResolver, request, response);
                headers.setLocation(URI.create(frontendUrl.concat(SLASH)));
//            return new ResponseEntity<>("User successfully logged in", Utils.createHeaders(), HttpStatus.OK);
                return new ResponseEntity<>(headers, HttpStatus.FOUND);
            }
        });
    }

    @GetMapping("/native/login/{profile-type}")
    public CompletableFuture<ResponseEntity<JsonNode>> nativeLogin(@PathVariable(name = "profile-type") String profileType, @RequestBody JsonNode requestBody,
                            HttpServletRequest request, HttpServletResponse response){
        log.info("Starting flow for native login for profile type: {}", profileType);
        HttpHeaders headers = Utils.createHeaders(request);
        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, "native-login", requestBody);
        return authService.performNativeLogin(apiRequestResolver, profileType).thenApplyAsync(apiResponseResolver -> {
            log.info("{} Received response from native login flow: {}", apiRequestResolver.getLoggerString(), apiResponseResolver);
            setRequiredCookies(apiRequestResolver, request, response);
            if(!apiResponseResolver.getStatusCode().equals(HttpStatus.FOUND)){
                return new ResponseEntity<>(apiResponseResolver.getRespData(), headers, apiResponseResolver.getStatusCode());
            }else{
                headers.setLocation(URI.create(frontendUrl.concat(SLASH)));
//            return new ResponseEntity<>("User successfully logged in", Utils.createHeaders(), HttpStatus.OK);
                return new ResponseEntity<>(headers, HttpStatus.FOUND);
            }
        });
    }

    @GetMapping("/validate-token")
    public ResponseEntity<JsonNode> validateToken(@AuthenticationPrincipal Authentication authentication, HttpServletRequest request, HttpServletResponse response){
        log.info("Starting flow for validating session token");
        HttpHeaders headers = Utils.createHeaders(request);
        ApiRequestResolver apiRequestResolver = getApiRequestResolver(authentication, request, "validate-token");
        if(!apiRequestResolver.isLoggedIn() || apiRequestResolver.getUserId()==null || apiRequestResolver.getUserId().isEmpty()){
            log.error("{} User not logged in or user id not found in session", apiRequestResolver.getLoggerString());
            ObjectNode responseData = mapper.createObjectNode();
            responseData.put(STATUS, FAILURE);
            responseData.put(MESSAGE, "No logged in user found in session");
            setRequiredCookies(apiRequestResolver, request, response);
            return new ResponseEntity<>(responseData, headers, HttpStatus.OK);
        }
        ObjectNode responseData = mapper.createObjectNode();
        responseData.put(STATUS, SUCCESS);
        responseData.put(MESSAGE, "Valid user found in session");
        setRequiredCookies(apiRequestResolver, request, response);
        return new ResponseEntity<>(responseData, headers, HttpStatus.OK);
    }

    @GetMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Authentication authentication, HttpServletRequest request, HttpServletResponse response){
        log.info("Starting flow for logging out user");
        HttpHeaders headers = Utils.createHeaders(request);
        ApiRequestResolver apiRequestResolver = getApiRequestResolver(authentication, request, "logout");
        if(!apiRequestResolver.isLoggedIn() || apiRequestResolver.getUserId()==null || apiRequestResolver.getUserId().isEmpty()){
            log.error("{} User not logged in or user id not found in session in order to do user logout", apiRequestResolver.getLoggerString());
            setRequiredCookies(apiRequestResolver, request, response);
            headers.setLocation(URI.create(frontendUrl.concat(QUESTION_MARK).concat("error=unauthorized")));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
        authService.logoutUser(apiRequestResolver);
        setRequiredCookies(apiRequestResolver, request, response);
        headers.setLocation(URI.create(frontendUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    private ApiRequestResolver getApiRequestResolver(Authentication authentication, HttpServletRequest request, String apiKey) {
        ApiRequestResolver apiRequestResolver;
        if(authentication == null || authentication.getDetails() == null){
            log.info("No authentication or auth details found in request for apiKey {}", apiKey);
            apiRequestResolver = commonService.createApiResolverRequest(request, apiKey, null);
            apiRequestResolver.setLoggedIn(false);
            apiRequestResolver.setUserId(null);
            return apiRequestResolver;
        }
        log.info("Authentication and auth details found in request for apiKey {}", apiKey);
        log.debug("Authentication details found in request for apiKey {} is {}", apiKey, authentication.getDetails());
        apiRequestResolver = (ApiRequestResolver) authentication.getDetails();
        apiRequestResolver.setApiKey(apiKey);
        apiRequestResolver.setRequestBody(null);

        String userIdByPrinciple = (String) authentication.getPrincipal();
        apiRequestResolver.setUserId(userIdByPrinciple);
        if(userIdByPrinciple==null || userIdByPrinciple.isEmpty()){
            apiRequestResolver.setLoggedIn(false);
        }else{
            apiRequestResolver.setLoggedIn(true);
        }
        return apiRequestResolver;
    }

    private void setRequiredCookies(ApiRequestResolver apiRequestResolver, HttpServletRequest request,
                                    HttpServletResponse response) {
        log.info("{} starting to set required session and user cookies for general api flows.", apiRequestResolver.getLoggerString());
        utils.setSessionAndCookieDataForSession(apiRequestResolver, request, response);
//        utils.setUserIdCookieNeededOrRemove(apiRequestResolver, request, response);
    }

}

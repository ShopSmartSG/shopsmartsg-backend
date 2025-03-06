package sg.edu.nus.iss.shopsmart_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import sg.edu.nus.iss.shopsmart_backend.model.*;
import sg.edu.nus.iss.shopsmart_backend.utils.Constants;
import sg.edu.nus.iss.shopsmart_backend.utils.RedisManager;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class AuthService extends Constants {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final ObjectMapper mapper = Json.mapper();

    @Value("${oauth2.client_id}")
    private String clientId;

    @Value("${oauth2.client_secret}")
    private String clientSecret;

    @Value("${oauth2.redirect_uri}")
    private String redirectUri;

    @Value("${oauth2.authorization_endpoint}")
    private String authorizationEndpoint;

    @Value("${oauth2.token_endpoint}")
    private String tokenEndpoint;

    // Example scope: openid and email are sufficient for authorization.
    @Value("${oauth2.scope}")
    private String scope;

    @Value("${gcip.api_key}")
    private String gcipApiKey;

    @Value("${gcip.request_uri}")
    private String gcipRequestUri;

    @Value("${gcip.sign_in_with_idp_url}")
    private String gcipSignInWithIdpUrl;

    @Value("${gcip.account_lookup_url}")
    private String gcipLookupUserUrl;

    @Value("${gcip.refresh_id_token_url}")
    private String gcipRefreshIdTokenUrl;

    @Value("${gcip.sign_up_native_url}")
    private String gcipSignUpNativeUrl;

    @Value("${gcip.login_native_url}")
    private String gcipLoginNativeUrl;

    @Value("${frontend_url}")
    private String frontendUrl;

    private final ProfileService profileService;
    private final CommonService commonService;
    private final RedisManager redisManager;
    RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public AuthService(ProfileService profileService, CommonService commonService, RedisManager redisManager) {
        this.profileService = profileService;
        this.commonService = commonService;
        this.redisManager = redisManager;
    }

    /**
     * Constructs the authorization URL that the user will be redirected to.
     */
    public String generateAuthorizationUrl(ApiRequestResolver apiRequestResolver, String profileType) {
        String state = UUID.randomUUID().toString();
        updateSessionInRedis(apiRequestResolver.getSessionId(), STATE, state, apiRequestResolver.getLoggerString());
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(authorizationEndpoint)
                .queryParam(CLIENT_ID, clientId)
                .queryParam(REDIRECT_URI, redirectUri.concat(SLASH).concat(profileType))
                .queryParam(RESPONSE_TYPE, CODE)
                .queryParam(SCOPE, scope)
                .queryParam(STATE, state);
        String authorizationUrl = builder.toUriString();
        log.info("{} Generated authorization URL: {}", apiRequestResolver.getLoggerString(), authorizationUrl);
        updateSessionInRedis(apiRequestResolver.getSessionId(), AUTHORIZATION_URL, authorizationUrl, apiRequestResolver.getLoggerString());
        updateSessionInRedis(apiRequestResolver.getSessionId(), PROFILE_TYPE, profileType, apiRequestResolver.getLoggerString());
        return authorizationUrl;
    }

    public boolean validateIfCallbackValueMatchesDataInSession(String sessionId, String key, String valueInCallback, String loggerString){
        log.info("{} Validating {} in callback from session for sessionId: {} with valueInCallback as {}", loggerString, key, sessionId, valueInCallback);
        String sessionKey = REDIS_SESSION_PREFIX.concat(sessionId);
        String valueInSession = redisManager.getHashValue(sessionKey, key);
        if(valueInSession != null && !valueInSession.isEmpty() && valueInSession.equals(valueInCallback)){
            return true;
        }
        log.info("{} {} validation failed, with value in session as: {} and valueInCallback as: {}", loggerString, key, valueInSession, valueInCallback);
        return false;
    }

    //this is basic OAuth2 flow to exchange code for tokens.
    public OAuth2TokenExchangeResponse exchangeCodeForTokens(ApiRequestResolver apiRequestResolver, String code, String profileType) {
        log.info("{} For current user starting oauth token exchange with code: {} and profileType: {}",
                apiRequestResolver.getLoggerString(), code, profileType);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(CODE, code);
//        params.add(REDIRECT_URI, redirectUri);
        params.add(REDIRECT_URI, redirectUri.concat(SLASH).concat(profileType));
        params.add(CLIENT_ID, clientId);
        params.add(CLIENT_SECRET, clientSecret);
        params.add(GRANT_TYPE, AUTHORIZATION_CODE);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(tokenEndpoint, HttpMethod.POST, request, JsonNode.class);
        log.info("{} Response from token exchange: {} with status {}", apiRequestResolver.getLoggerString(), response.getBody(), response.getStatusCode());
        if(!response.hasBody() || response.getBody()==null){
            log.error("{} Error in token exchange, response body is null", apiRequestResolver.getLoggerString());
            return null;
        }
        if(response.getStatusCode() == HttpStatus.BAD_REQUEST || (response.getBody()!=null && response.getBody().hasNonNull(ERROR))){
            log.info("{} Invalid grant received in token exchange response with statusCode : {}", apiRequestResolver.getLoggerString(), response.getStatusCode());
            //will only store raw responses for error cases.
            updateSessionInRedis(apiRequestResolver.getSessionId(), RAW_GOOGLE_TOKEN_EXCHANGE_RESP,
                    response.getBody().toString(), apiRequestResolver.getLoggerString());
            OAuth2TokenExchangeResponse resp = new OAuth2TokenExchangeResponse();
            resp.setInvalidGrant(true);
            return resp;
        }

        return mapper.convertValue(response.getBody(), OAuth2TokenExchangeResponse.class);
    }

    //here we register/login a user in GCIP using the id token received from OAuth2 flow.
    public GcipSignInWithIdpTokenResponse signWithIdpThroughGcip(ApiRequestResolver apiRequestResolver, String accessToken){
        log.info("{} For current user starting sign in with idp through GCIP using accessToken: {}", apiRequestResolver.getLoggerString(), accessToken);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        // Create the "postBody" parameter as a URL-encoded string.
//        String postBody = "access_token=" + accessToken + "&providerId=google.com";
        String postBody = ACCESS_TOKEN.concat(EQUALS).concat(accessToken).concat(AMPERSAND).concat(PROVIDER_ID_GOOGLE);
        body.put(POST_BODY, postBody);
        body.put("requestUri", gcipRequestUri);
        body.put("returnIdpCredential", true);
        body.put("returnSecureToken", true);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        String url = gcipSignInWithIdpUrl.concat(gcipApiKey);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);

        log.info("{} Response from signInWithIdp: {} with status {}", apiRequestResolver.getLoggerString(), response.getBody(), response.getStatusCode());
        if(!response.hasBody() || response.getBody()==null){
            log.error("{} Error in sign in with idp through GCIP, response body is null", apiRequestResolver.getLoggerString());
            return null;
        }
        if(response.getStatusCode() == HttpStatus.BAD_REQUEST || (response.getBody()!=null && response.getBody().hasNonNull(ERROR))){
            log.info("{} Invalid input response received during signInWithIdp response with statusCode : {}", apiRequestResolver.getLoggerString(), response.getStatusCode());
            //will only store raw responses for error cases.
            updateSessionInRedis(apiRequestResolver.getSessionId(), RAW_SIGN_WITH_IDP_THROUGH_GCIP_RESP,
                    response.getBody().toString(), apiRequestResolver.getLoggerString());
            GcipSignInWithIdpTokenResponse resp = new GcipSignInWithIdpTokenResponse();
            resp.setInvalid(true);
            return resp;
        }

        return mapper.convertValue(response.getBody(), GcipSignInWithIdpTokenResponse.class);
    }

    public GcipAccLookupResponse lookupExistingUserThroughGcip(String idToken, String sessionId, String loggerString) {
        log.info("{} Looking up existing user through GCIP using idToken: {} in session {}", loggerString, idToken, sessionId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("idToken", idToken);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

        String url = gcipLookupUserUrl.concat(gcipApiKey);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.POST, requestEntity, JsonNode.class);

        log.info("{} User Account lookup response: {} with status {}", loggerString, response.getBody(), response.getStatusCode());
        if(!response.hasBody() || response.getBody()==null){
            log.error("{} Error in looking existing user through Gcip, response body is null", loggerString);
            return null;
        }
        if(response.getStatusCode() == HttpStatus.BAD_REQUEST || (response.getBody()!=null && response.getBody().hasNonNull(ERROR))){
            log.info("{} Lookup response says id_token is expired, needs to be refreshed, statusCode : {}", loggerString, response.getStatusCode());
            //will only store raw responses for error cases.
            updateSessionInRedis(sessionId, RAW_ACC_LOOKUP_THROUGH_GCIP_RESP,
                    response.getBody().toString(), loggerString);
            GcipAccLookupResponse resp = new GcipAccLookupResponse();
            resp.setIdTokenRefreshNeeded(true);
            return resp;
        }

        return mapper.convertValue(response.getBody(), GcipAccLookupResponse.class);
    }

    public GcipRefreshTokenResponse refreshIdTokenThroughGcip(String refreshToken, String sessionId, String loggerString){
        log.info("{} Refreshing id token through GCIP using refreshToken: {}", loggerString, refreshToken);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        String url = gcipRefreshIdTokenUrl.concat(gcipApiKey);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.POST, request, JsonNode.class);

        log.info("Refresh id token response: {} with status {}", response.getBody(), response.getStatusCode());
        if(!response.hasBody() || response.getBody()==null){
            log.error("{} Error occurred while refreshing id_token through Gcip, response body is null", loggerString);
            return null;
        }
        if(response.getStatusCode() == HttpStatus.BAD_REQUEST || (response.getBody()!=null && response.getBody().hasNonNull(ERROR))){
            log.info("{} Unable to refresh id_refresh due to invalid refresh_token or expired, statusCode : {}", loggerString, response.getStatusCode());
            //will only store raw responses for error cases.
            updateSessionInRedis(sessionId, RAW_REFRESH_ID_TOKEN_THROUGH_GCIP,
                    response.getBody().toString(), loggerString);
            GcipRefreshTokenResponse resp = new GcipRefreshTokenResponse();
            resp.setInvalidRefreshToken(true);
            return resp;
        }

        return mapper.convertValue(response.getBody(), GcipRefreshTokenResponse.class);
    }

    public GcipNativeLoginTokenResp nativeAuthForUserThroughGcip(String email, String password, String sessionId, String profileType,
                                                                 boolean isSignUpFlow, String loggerString){
        log.info("{} Signing up user through GCIP using email: {} for profileType: {}", loggerString, email, profileType);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put(EMAIL, email);
        body.put(PASSWORD, password);
        body.put(RETURN_SECURE_TOKEN, "true");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        String url = isSignUpFlow ? gcipSignUpNativeUrl.concat(gcipApiKey) : gcipLoginNativeUrl.concat(gcipApiKey);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.POST, requestEntity, JsonNode.class);

        log.info("{} User native login for when signUp is {}, call response: {} with status {}",
                loggerString, isSignUpFlow, response.getBody(), response.getStatusCode());
        if(!response.hasBody() || response.getBody()==null){
            log.error("{} Error occurred while native login through Gcip, response body is null", loggerString);
            return null;
        }
        if(response.getStatusCode() == HttpStatus.BAD_REQUEST || (response.getBody()!=null && response.getBody().hasNonNull(ERROR))){
            log.info("{} unable to get tokens to due to invalid credentials or user already exists, statusCode : {} and signUp flow is {}",
                    loggerString, response.getStatusCode(), isSignUpFlow);
            //will only store raw responses for error cases.
            updateSessionInRedis(sessionId, RAW_NATIVE_LOGIN_THROUGH_GCIP_RESP,
                    response.getBody().toString(), loggerString);
            GcipNativeLoginTokenResp resp = new GcipNativeLoginTokenResp();
            resp.setInvalidCredentials(true);
            return resp;
        }

        return mapper.convertValue(response.getBody(), GcipNativeLoginTokenResp.class);
    }

    public CompletableFuture<URI> performGoogleCallbackHandling(ApiRequestResolver apiRequestResolver, String code, String state, String profileType){
        log.info("{} Starting google callback handling for profileType: {}, state: {} and code: {}",
                apiRequestResolver.getLoggerString(), profileType, state, code);
        //validate state value from session in redis.
        if(state == null || state.isEmpty() ||
                !validateIfCallbackValueMatchesDataInSession(apiRequestResolver.getSessionId(),
                        STATE, state, apiRequestResolver.getLoggerString())){
            log.error("{} Invalid state value received in callback for google login", apiRequestResolver.getLoggerString());
//            return CompletableFuture.completedFuture(new ResponseEntity<>(headers, HttpStatus.FOUND));
            return CompletableFuture.completedFuture(URI.create(frontendUrl.concat(SLASH)
                    .concat("login").concat(QUESTION_MARK).concat("error=invalidstate")));
        }

        if(profileType == null || profileType.isEmpty() ||
                !validateIfCallbackValueMatchesDataInSession(apiRequestResolver.getSessionId(),
                        PROFILE_TYPE, profileType, apiRequestResolver.getLoggerString())){
            log.error("{} Invalid profile type received in callback for google login", apiRequestResolver.getLoggerString());
//            return CompletableFuture.completedFuture(new ResponseEntity<>(headers, HttpStatus.FOUND));
            return CompletableFuture.completedFuture(URI.create(frontendUrl.concat(SLASH)
                    .concat("login").concat(QUESTION_MARK).concat("error=invalidprofiletype")));
        }

        OAuth2TokenExchangeResponse accessTokenResp = exchangeCodeForTokens(apiRequestResolver, code, profileType);
        if(accessTokenResp == null){
            log.error("{} Exception occurred in fetching access token from google", apiRequestResolver.getLoggerString());
//            return CompletableFuture.completedFuture(new ResponseEntity<>(headers, HttpStatus.FOUND));
            return CompletableFuture.completedFuture(URI.create(frontendUrl.concat(SLASH)
                    .concat("login").concat(QUESTION_MARK).concat("error=loginfailed")));
        }
        if(accessTokenResp.isInvalidGrant()){
            log.error("{} Invalid grant received in token exchange response", apiRequestResolver.getLoggerString());
            return CompletableFuture.completedFuture(URI.create(frontendUrl.concat(SLASH)
                    .concat("login").concat(QUESTION_MARK).concat("error=loginagain")));
        }
        log.debug("Access token received with access token value {}", accessTokenResp.getAccessToken());
        log.debug("Access token received with id token {}", accessTokenResp.getIdToken());

        GcipSignInWithIdpTokenResponse gcipSignInWithIdpTokenResponse = signWithIdpThroughGcip(apiRequestResolver, accessTokenResp.getAccessToken());
        if (gcipSignInWithIdpTokenResponse == null){
            log.error("{} Error in fetching user profile from GCIP", apiRequestResolver.getLoggerString());
//            return CompletableFuture.completedFuture(new ResponseEntity<>(headers, HttpStatus.FOUND));
            return CompletableFuture.completedFuture(URI.create(frontendUrl.concat(SLASH)
                    .concat("login").concat(QUESTION_MARK).concat("error=userauthfailed")));
        }
        if(gcipSignInWithIdpTokenResponse.isInvalid()){
            log.error("{} Invalid input response received during signInWithIdp response", apiRequestResolver.getLoggerString());
            return CompletableFuture.completedFuture(URI.create(frontendUrl.concat(SLASH)
                    .concat("login").concat(QUESTION_MARK).concat("error=tryloginagain")));
        }
        log.debug("Email received from GCIP: {}", gcipSignInWithIdpTokenResponse.getEmail());
        log.debug("Id token received from GCIP: {}", gcipSignInWithIdpTokenResponse.getIdToken());
        log.debug("Refresh token received from GCIP: {}", gcipSignInWithIdpTokenResponse.getRefreshToken());

        return profileService.fetchUserIdForEmail(apiRequestResolver, gcipSignInWithIdpTokenResponse.getEmail(), profileType).thenApplyAsync(profileUserIdResp -> {
            if(profileUserIdResp == null || profileUserIdResp.isEmpty()){
                log.error("{} Error in fetching user profile from profile service", apiRequestResolver.getLoggerString());
//                return new ResponseEntity<>(headers, HttpStatus.FOUND);
                return URI.create(frontendUrl.concat(SLASH).concat("login").concat(QUESTION_MARK).concat("error=unabletologin"));
            }
            log.info("User profile id fetched from profile service: {}", profileUserIdResp);

            updateSessionInRedis(apiRequestResolver.getSessionId(), GCIP_ID_TOKEN,
                    gcipSignInWithIdpTokenResponse.getIdToken(), apiRequestResolver.getLoggerString());
            updateSessionInRedis(apiRequestResolver.getSessionId(), GCIP_REFRESH_TOKEN,
                    gcipSignInWithIdpTokenResponse.getRefreshToken(), apiRequestResolver.getLoggerString());
            updateSessionInRedis(apiRequestResolver.getSessionId(), OAUTH_ID_TOKEN,
                    accessTokenResp.getIdToken(), apiRequestResolver.getLoggerString());
            updateSessionInRedis(apiRequestResolver.getSessionId(), OAUTH_ACCESS_TOKEN,
                    accessTokenResp.getAccessToken(), apiRequestResolver.getLoggerString());
            updateSessionInRedis(apiRequestResolver.getSessionId(), EMAIL,
                    gcipSignInWithIdpTokenResponse.getEmail(), apiRequestResolver.getLoggerString());
            updateSessionInRedis(apiRequestResolver.getSessionId(), LOGIN_TYPE, GOOGLE, apiRequestResolver.getLoggerString());

            apiRequestResolver.setUserId(profileUserIdResp);
            apiRequestResolver.setLoggedIn(true);
            commonService.updateUserIdInRedisInSessionData(apiRequestResolver);
            return URI.create(frontendUrl.concat(SLASH));
//            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        });
    }

    public CompletableFuture<ApiResponseResolver> performNativeSignUp(ApiRequestResolver apiRequestResolver, String profileType){
        log.info("{} Starting native sign up flow for profileType: {}", apiRequestResolver.getLoggerString(), profileType);
        JsonNode requestBody = apiRequestResolver.getRequestBody();
        ObjectNode responseData = mapper.createObjectNode();
        ApiResponseResolver apiResponseResolver = new ApiResponseResolver();
        if(requestBody == null || !requestBody.hasNonNull(EMAIL) || !requestBody.hasNonNull(PASSWORD)){
            log.error("{} Email or password not found in request body for signUp", apiRequestResolver.getLoggerString());
            responseData.put(STATUS, FAILURE);
            responseData.put(MESSAGE, "Email or password not found in request body");
            apiResponseResolver.setStatusCode(HttpStatus.BAD_REQUEST);
            apiResponseResolver.setRespData(responseData);
            return CompletableFuture.completedFuture(apiResponseResolver);
        }
        String email = requestBody.get(EMAIL).asText();
        String password = requestBody.get(PASSWORD).asText();
        GcipNativeLoginTokenResp gcipNativeSignUpTokenResp = nativeAuthForUserThroughGcip(email, password,
                apiRequestResolver.getLoggerString(), profileType, true, apiRequestResolver.getLoggerString());
        if (gcipNativeSignUpTokenResp == null){
            log.error("{} Error occurred while trying to register user creds with GCIP for signUp", apiRequestResolver.getLoggerString());
            responseData.put(STATUS, FAILURE);
            responseData.put(MESSAGE, "Unable to register user credentials with GCIP or user already exists");
            apiResponseResolver.setStatusCode(HttpStatus.BAD_REQUEST);
            apiResponseResolver.setRespData(responseData);
            return CompletableFuture.completedFuture(apiResponseResolver);
        }
        if(gcipNativeSignUpTokenResp.isInvalidCredentials()){
            log.error("{} Unable to register user with GCIP due to user possibily already exists", apiRequestResolver.getLoggerString());
            responseData.put(STATUS, FAILURE);
            responseData.put(MESSAGE, "Unable to register user with GCIP, user already exists");
            apiResponseResolver.setStatusCode(HttpStatus.BAD_REQUEST);
            apiResponseResolver.setRespData(responseData);
            return CompletableFuture.completedFuture(apiResponseResolver);
        }
        log.debug("Email received from GCIP for native signUp resp: {}", gcipNativeSignUpTokenResp.getEmail());
        log.debug("Id token received from GCIP for native signUp resp: {}", gcipNativeSignUpTokenResp.getIdToken());
        log.debug("Refresh token received from GCIP for native signUp resp: {}", gcipNativeSignUpTokenResp.getRefreshToken());

        return profileService.createProfile(apiRequestResolver, profileType).thenComposeAsync(createProfileResp -> {
            if(!createProfileResp){
                log.error("{} Error in creating user profile for native sign up flow", apiRequestResolver.getLoggerString());
                responseData.put(STATUS, FAILURE);
                responseData.put(MESSAGE, "Unable to register user profile or user already exists");
                apiResponseResolver.setStatusCode(HttpStatus.BAD_REQUEST);
                apiResponseResolver.setRespData(responseData);
                return CompletableFuture.completedFuture(apiResponseResolver);
            }
            log.info("{} User profile created successfully for native sign up flow", apiRequestResolver.getLoggerString());
            //TODO :: incorporate OTP validation part here as well.
            return profileService.fetchUserIdForEmail(apiRequestResolver, gcipNativeSignUpTokenResp.getEmail(), profileType).thenApplyAsync(profileUserIdResp -> {
                if(profileUserIdResp == null || profileUserIdResp.isEmpty()){
                    log.error("{} Error in fetching user profile from profile service post profile creation", apiRequestResolver.getLoggerString());
                    responseData.put(STATUS, FAILURE);
                    responseData.put(MESSAGE, "Unable to fetch profile post register");
                    apiResponseResolver.setStatusCode(HttpStatus.BAD_REQUEST);
                    apiResponseResolver.setRespData(responseData);
                    return apiResponseResolver;
                }
                log.info("User profile id fetched from profile service for native signUp flow: {}", profileUserIdResp);

                updateSessionInRedis(apiRequestResolver.getSessionId(), GCIP_ID_TOKEN,
                        gcipNativeSignUpTokenResp.getIdToken(), apiRequestResolver.getLoggerString());
                updateSessionInRedis(apiRequestResolver.getSessionId(), GCIP_REFRESH_TOKEN,
                        gcipNativeSignUpTokenResp.getRefreshToken(), apiRequestResolver.getLoggerString());
                updateSessionInRedis(apiRequestResolver.getSessionId(), EMAIL,
                        gcipNativeSignUpTokenResp.getEmail(), apiRequestResolver.getLoggerString());
                updateSessionInRedis(apiRequestResolver.getSessionId(), LOGIN_TYPE, NATIVE, apiRequestResolver.getLoggerString());

                apiRequestResolver.setUserId(profileUserIdResp);
                apiRequestResolver.setLoggedIn(true);
                commonService.updateUserIdInRedisInSessionData(apiRequestResolver);
                apiResponseResolver.setStatusCode(HttpStatus.FOUND);
                return apiResponseResolver;
            });
        });
    }

    public CompletableFuture<ApiResponseResolver> performNativeLogin(ApiRequestResolver apiRequestResolver, String profileType){
        log.info("{} Starting native login flow for profileType: {}", apiRequestResolver.getLoggerString(), profileType);
        JsonNode requestBody = apiRequestResolver.getRequestBody();
        ObjectNode responseData = mapper.createObjectNode();
        ApiResponseResolver apiResponseResolver = new ApiResponseResolver();
        if(requestBody == null || !requestBody.hasNonNull(EMAIL) || !requestBody.hasNonNull(PASSWORD)){
            log.error("{} Email or password not found in request body for login", apiRequestResolver.getLoggerString());
            responseData.put(STATUS, FAILURE);
            responseData.put(MESSAGE, "Email or password not found in request body");
            apiResponseResolver.setStatusCode(HttpStatus.BAD_REQUEST);
            apiResponseResolver.setRespData(responseData);
            return CompletableFuture.completedFuture(apiResponseResolver);
        }
        String email = requestBody.get(EMAIL).asText();
        String password = requestBody.get(PASSWORD).asText();
        GcipNativeLoginTokenResp gcipNativeLoginTokenResp = nativeAuthForUserThroughGcip(email, password,
                apiRequestResolver.getLoggerString(), profileType, false, apiRequestResolver.getLoggerString());
        if (gcipNativeLoginTokenResp == null){
            log.error("{} Error occurred while trying to validate user creds with GCIP", apiRequestResolver.getLoggerString());
            responseData.put(STATUS, FAILURE);
            responseData.put(MESSAGE, "Unable to validate user credentials with GCIP");
            apiResponseResolver.setStatusCode(HttpStatus.BAD_REQUEST);
            apiResponseResolver.setRespData(responseData);
            return CompletableFuture.completedFuture(apiResponseResolver);
        }
        if(gcipNativeLoginTokenResp.isInvalidCredentials()){
            log.error("{} Unable to validate user with GCIP due to invalid credentials", apiRequestResolver.getLoggerString());
            responseData.put(STATUS, FAILURE);
            responseData.put(MESSAGE, "Unable to validate user with GCIP, invalid credentials");
            apiResponseResolver.setStatusCode(HttpStatus.BAD_REQUEST);
            apiResponseResolver.setRespData(responseData);
            return CompletableFuture.completedFuture(apiResponseResolver);
        }
        log.debug("Email received from GCIP for native login resp: {}", gcipNativeLoginTokenResp.getEmail());
        log.debug("Id token received from GCIP for native login resp: {}", gcipNativeLoginTokenResp.getIdToken());
        log.debug("Refresh token received from GCIP for native login resp: {}", gcipNativeLoginTokenResp.getRefreshToken());

        return profileService.fetchUserIdForEmail(apiRequestResolver, gcipNativeLoginTokenResp.getEmail(), profileType).thenApplyAsync(profileUserIdResp -> {
            if(profileUserIdResp == null || profileUserIdResp.isEmpty()){
                log.error("{} Error in fetching user profile from profile service for native login flow", apiRequestResolver.getLoggerString());
                responseData.put(STATUS, FAILURE);
                responseData.put(MESSAGE, "Unable to fetch user profile for login");
                apiResponseResolver.setStatusCode(HttpStatus.BAD_REQUEST);
                apiResponseResolver.setRespData(responseData);
                return apiResponseResolver;
            }
            log.info("User profile id fetched from profile service for native login flow: {}", profileUserIdResp);
            //TODO :: incorporate OTP validation part here as well.

            updateSessionInRedis(apiRequestResolver.getSessionId(), GCIP_ID_TOKEN,
                    gcipNativeLoginTokenResp.getIdToken(), apiRequestResolver.getLoggerString());
            updateSessionInRedis(apiRequestResolver.getSessionId(), GCIP_REFRESH_TOKEN,
                    gcipNativeLoginTokenResp.getRefreshToken(), apiRequestResolver.getLoggerString());
            updateSessionInRedis(apiRequestResolver.getSessionId(), EMAIL,
                    gcipNativeLoginTokenResp.getEmail(), apiRequestResolver.getLoggerString());
            updateSessionInRedis(apiRequestResolver.getSessionId(), LOGIN_TYPE, NATIVE, apiRequestResolver.getLoggerString());

            apiRequestResolver.setUserId(profileUserIdResp);
            apiRequestResolver.setLoggedIn(true);
            commonService.updateUserIdInRedisInSessionData(apiRequestResolver);
            apiResponseResolver.setStatusCode(HttpStatus.FOUND);
            return apiResponseResolver;
        });
    }

    public void logoutUser(ApiRequestResolver apiRequestResolver){
        log.info("{} Logging out user: {}", apiRequestResolver.getLoggerString(), apiRequestResolver.getUserId());
        deleteFromSessionInRedis(apiRequestResolver.getSessionId(), USER_ID, apiRequestResolver.getLoggerString());
        deleteFromSessionInRedis(apiRequestResolver.getSessionId(), EMAIL, apiRequestResolver.getLoggerString());
        deleteFromSessionInRedis(apiRequestResolver.getSessionId(), GCIP_ID_TOKEN, apiRequestResolver.getLoggerString());
        deleteFromSessionInRedis(apiRequestResolver.getSessionId(), GCIP_REFRESH_TOKEN, apiRequestResolver.getLoggerString());
        deleteFromSessionInRedis(apiRequestResolver.getSessionId(), OAUTH_ACCESS_TOKEN, apiRequestResolver.getLoggerString());
        deleteFromSessionInRedis(apiRequestResolver.getSessionId(), OAUTH_ID_TOKEN, apiRequestResolver.getLoggerString());
        deleteFromSessionInRedis(apiRequestResolver.getSessionId(), AUTHORIZATION_URL, apiRequestResolver.getLoggerString());
        deleteFromSessionInRedis(apiRequestResolver.getSessionId(), PROFILE_TYPE, apiRequestResolver.getLoggerString());
        deleteFromSessionInRedis(apiRequestResolver.getSessionId(), LOGIN_TYPE, apiRequestResolver.getLoggerString());
        deleteFromSessionInRedis(apiRequestResolver.getSessionId(), IS_LOGGED_IN, apiRequestResolver.getLoggerString());
        deleteFromSessionInRedis(apiRequestResolver.getSessionId(), STATE, apiRequestResolver.getLoggerString());
        deleteFromSessionInRedis(apiRequestResolver.getSessionId(), RAW_GOOGLE_TOKEN_EXCHANGE_RESP, apiRequestResolver.getLoggerString());
        deleteFromSessionInRedis(apiRequestResolver.getSessionId(), RAW_SIGN_WITH_IDP_THROUGH_GCIP_RESP, apiRequestResolver.getLoggerString());
        deleteFromSessionInRedis(apiRequestResolver.getSessionId(), RAW_ACC_LOOKUP_THROUGH_GCIP_RESP, apiRequestResolver.getLoggerString());
        deleteFromSessionInRedis(apiRequestResolver.getSessionId(), RAW_REFRESH_ID_TOKEN_THROUGH_GCIP, apiRequestResolver.getLoggerString());
        deleteFromSessionInRedis(apiRequestResolver.getSessionId(), RAW_NATIVE_LOGIN_THROUGH_GCIP_RESP, apiRequestResolver.getLoggerString());
        apiRequestResolver.setUserId(null);
        apiRequestResolver.setLoggedIn(false);
        log.info("{} all redis session operations completed successfully", apiRequestResolver.getLoggerString());
    }

    public void deleteFromSessionInRedis(String sessionId, String key, String loggerString){
        log.info("{} Deleting from session in redis for: {}", loggerString, key);
        String sessionKey = REDIS_SESSION_PREFIX.concat(sessionId);
        log.info("{} Deleting session data in redis key: {}", loggerString, sessionKey);
        redisManager.delHashValue(sessionKey, key);
    }

    public void updateSessionInRedis(String sessionId, String key, String value, String loggerString){
        log.info("{} Updating in session in redis for: {} with data: {}", loggerString, key, value);
        String sessionKey = REDIS_SESSION_PREFIX.concat(sessionId);
        log.info("{} Updating session data in redis key: {}", loggerString, sessionKey);
        redisManager.setHashValue(sessionKey, key, value);

//        log.info("{} Fetching session data from redis key: {}", loggerString, sessionKey);
//        Map<String, String> sessionData = redisManager.getHashMap(sessionKey);
//        log.debug("{} Session data fetched : {}", loggerString, sessionData);
//        if(sessionData == null){
//            sessionData = new HashMap<>();
//        }
//        sessionData.put(key, value);
//        apiRequestResolver.setSessionAttributes(sessionData);
    }


}

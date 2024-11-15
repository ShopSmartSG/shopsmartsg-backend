package sg.edu.nus.iss.shopsmart_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.model.ApiResponseResolver;
import sg.edu.nus.iss.shopsmart_backend.model.DataDynamicObject;
import sg.edu.nus.iss.shopsmart_backend.utils.*;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

@Service
public class ProfileService extends Constants {
    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);
    private final ObjectMapper mapper = Json.mapper();

    private final RedisManager redisManager;
    private final WSUtils wsUtils;

    @Autowired
    public ProfileService(RedisManager redisManager, WSUtils wsUtils){
        this.redisManager = redisManager;
        this.wsUtils = wsUtils;
    }

    public CompletableFuture<ApiResponseResolver> generateOtpForRegister(ApiRequestResolver apiRequestResolver, String profileType){
        log.info("{} Generating OTP for profile registration with requestObt : {}", apiRequestResolver.getLoggerString(), apiRequestResolver.getRequestBody());
        ApiResponseResolver apiResponseResolver = new ApiResponseResolver();
        ObjectNode data = mapper.createObjectNode();
        JsonNode payload = apiRequestResolver.getRequestBody();
        if(payload.isNull() || payload.isEmpty()){
            log.info("{} No request body found for fetching user id for email", apiRequestResolver.getLoggerString());
            apiResponseResolver.setStatusCode(HttpStatus.BAD_REQUEST); // 400 bad request
            data.put(MESSAGE, "No request body found for otp generation for email");
            apiResponseResolver.setRespData(data);
            return CompletableFuture.completedFuture(apiResponseResolver);
        }
        if(!payload.hasNonNull(EMAIL)){
            log.info("{} No Email provided for generating OTP for registration", apiRequestResolver.getLoggerString());
            apiResponseResolver.setStatusCode(HttpStatus.BAD_REQUEST); // 400 bad request
            data.put(MESSAGE, "Email is required for generating OTP for registration");
            apiResponseResolver.setRespData(data);
            return CompletableFuture.completedFuture(apiResponseResolver);
        }
        String email = payload.get(EMAIL).asText();
        if(ADMIN.equalsIgnoreCase(profileType) && !checkIfValidAdminEmailId(email)){
            log.info("{} Email {} is not valid for admin, not authorized", apiRequestResolver.getLoggerString(), email);
            apiResponseResolver.setStatusCode(HttpStatus.UNAUTHORIZED); // 401 unauthorized
            data.put(MESSAGE, "Email is not authorized for admin OTP generation");
            apiResponseResolver.setRespData(data);
            return CompletableFuture.completedFuture(apiResponseResolver);
        }
        return fetchUserIdForEmail(apiRequestResolver, email, profileType).thenComposeAsync(userId -> {
            if(StringUtils.isNotEmpty(userId)){
                log.info("{} User id {} found for email, so not generating OTP for registration, user needs to login",
                        apiRequestResolver.getLoggerString(), userId);
                apiResponseResolver.setStatusCode(HttpStatus.CONFLICT); // 409 conflict
                data.put(MESSAGE, "User already exists with email, please login");
                apiResponseResolver.setRespData(data);
                return CompletableFuture.completedFuture(apiResponseResolver);
            }
            return generateOtp(apiRequestResolver, email);
        });
    }
    public CompletableFuture<ApiResponseResolver> generateOtpForLogin(ApiRequestResolver apiRequestResolver, String profileType){
        log.info("{} Generating OTP for profile login with requestObt : {}", apiRequestResolver.getLoggerString(), apiRequestResolver.getRequestBody());
        ApiResponseResolver apiResponseResolver = new ApiResponseResolver();
        ObjectNode data = mapper.createObjectNode();
        JsonNode payload = apiRequestResolver.getRequestBody();
        if(payload.isNull() || payload.isEmpty()){
            log.info("{} No request body found for opt generation for login", apiRequestResolver.getLoggerString());
            apiResponseResolver.setStatusCode(HttpStatus.BAD_REQUEST); // 400 bad request
            data.put(MESSAGE, "No request body found for otp generation for email");
            apiResponseResolver.setRespData(data);
            return CompletableFuture.completedFuture(apiResponseResolver);
        }
        if(!payload.hasNonNull(EMAIL)){
            log.info("{} No Email provided for generating OTP for login", apiRequestResolver.getLoggerString());
            apiResponseResolver.setStatusCode(HttpStatus.BAD_REQUEST); // 400 bad request
            data.put(MESSAGE, "Email is required for generating OTP for login");
            apiResponseResolver.setRespData(data);
            return CompletableFuture.completedFuture(apiResponseResolver);
        }
        String email = payload.get(EMAIL).asText();
        return fetchUserIdForEmail(apiRequestResolver, email, profileType).thenComposeAsync(userId -> {
            if(userId==null || StringUtils.isEmpty(userId)){
                log.info("{} No userId found for email, so not generating OTP for login, user accounts needs to be present to login",
                        apiRequestResolver.getLoggerString());
                apiResponseResolver.setStatusCode(HttpStatus.NOT_FOUND); // 404 not found
                data.put(MESSAGE, "User does not exists with email, please register");
                apiResponseResolver.setRespData(data);
                return CompletableFuture.completedFuture(apiResponseResolver);
            }
            return generateOtp(apiRequestResolver, email);
        });
    }
    public CompletableFuture<ApiResponseResolver> validateOtpAndRegister(ApiRequestResolver apiRequestResolver, String profileType){
        log.info("{} validating OTP for profile register with requestObt : {}", apiRequestResolver.getLoggerString(), apiRequestResolver.getRequestBody());
        ApiResponseResolver apiResponseResolver = new ApiResponseResolver();
        ObjectNode data = mapper.createObjectNode();
        JsonNode payload = apiRequestResolver.getRequestBody();
        if(payload.isNull() || payload.isEmpty()){
            log.info("{} No request body found for OTP validation for registration", apiRequestResolver.getLoggerString());
            apiResponseResolver.setStatusCode(HttpStatus.BAD_REQUEST); // 400 bad request
            data.put(MESSAGE, "No request body found for otp validation of email");
            apiResponseResolver.setRespData(data);
            return CompletableFuture.completedFuture(apiResponseResolver);
        }
        if(!payload.hasNonNull(EMAIL) || !payload.hasNonNull(OTP)){
            log.info("{} No Email or OTP provided for validating OTP for registration", apiRequestResolver.getLoggerString());
            apiResponseResolver.setStatusCode(HttpStatus.BAD_REQUEST); // 400 bad request
            data.put(MESSAGE, "Email and OTP are required for validating OTP for registration");
            apiResponseResolver.setRespData(data);
            return CompletableFuture.completedFuture(apiResponseResolver);
        }
        String email = payload.get(EMAIL).asText();
        String otp = payload.get(OTP).asText();
        return validateOtp(apiRequestResolver, email, otp).thenComposeAsync(isValid -> {
            log.info("{} OTP validation status for email for registration: {} is : {}", apiRequestResolver.getLoggerString(), email, isValid);
            if(isValid){
                log.info("{} as otp is valid, so registering user for email: {}", apiRequestResolver.getLoggerString(), email);
                return createProfileAndRetrieveId(apiRequestResolver, email, profileType);
            }else{
                log.error("{} OTP validation failed for email during registration: {}", apiRequestResolver.getLoggerString(), email);
                apiResponseResolver.setStatusCode(HttpStatus.UNAUTHORIZED); // 401 unauthorized
                data.put(MESSAGE, "OTP validation failed, please try again");
                apiResponseResolver.setRespData(data);
                return CompletableFuture.completedFuture(apiResponseResolver);
            }
        });
    }
    public CompletableFuture<ApiResponseResolver> validateOtpAndLogin(ApiRequestResolver apiRequestResolver, String profileType){
        log.info("{} validating OTP for profile login with requestObt : {}", apiRequestResolver.getLoggerString(), apiRequestResolver.getRequestBody());
        ApiResponseResolver apiResponseResolver = new ApiResponseResolver();
        ObjectNode data = mapper.createObjectNode();
        JsonNode payload = apiRequestResolver.getRequestBody();
        if(payload.isNull() || payload.isEmpty()){
            log.info("{} No request body found for OTP validation for login", apiRequestResolver.getLoggerString());
            apiResponseResolver.setStatusCode(HttpStatus.BAD_REQUEST); // 400 bad request
            data.put(MESSAGE, "No request body found for otp validation of email");
            apiResponseResolver.setRespData(data);
            return CompletableFuture.completedFuture(apiResponseResolver);
        }
        if(!payload.hasNonNull(EMAIL) || !payload.hasNonNull(OTP)){
            log.info("{} No Email or OTP provided for validating OTP for login", apiRequestResolver.getLoggerString());
            apiResponseResolver.setStatusCode(HttpStatus.BAD_REQUEST); // 400 bad request
            data.put(MESSAGE, "Email and OTP are required for validating OTP for login");
            apiResponseResolver.setRespData(data);
            return CompletableFuture.completedFuture(apiResponseResolver);
        }
        String email = payload.get(EMAIL).asText();
        String otp = payload.get(OTP).asText();
        return validateOtp(apiRequestResolver, email, otp).thenComposeAsync(isValid -> {
            log.info("{} OTP validation status for email: {} is : {}", apiRequestResolver.getLoggerString(), email, isValid);
            if(isValid){
                log.info("{} as otp is valid, so retrieving userId for email: {}", apiRequestResolver.getLoggerString(), email);
                return fetchUserIdForEmail(apiRequestResolver, email, profileType).thenComposeAsync(userId -> {
                    if(StringUtils.isEmpty(userId)){
                        log.error("{} No userId found for email: {} after OTP validation", apiRequestResolver.getLoggerString(), email);
                        apiResponseResolver.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR); // 500 internal server error
                        data.put(MESSAGE, "Some error occurred while fetching userId, please try again.");
                        apiResponseResolver.setRespData(data);
                        return CompletableFuture.completedFuture(apiResponseResolver);
                    }
                    log.info("{} User id found for email: {} after OTP validation, so returning userId", apiRequestResolver.getLoggerString(), email);
                    apiResponseResolver.setStatusCode(HttpStatus.OK); // 200 ok
                    data.put(USER_ID, userId);
                    apiResponseResolver.setRespData(data);
                    return CompletableFuture.completedFuture(apiResponseResolver);
                });
            }else{
                log.error("{} OTP validation failed for email during login: {}", apiRequestResolver.getLoggerString(), email);
                apiResponseResolver.setStatusCode(HttpStatus.UNAUTHORIZED); // 401 unauthorized
                data.put(MESSAGE, "OTP validation failed, please try again");
                apiResponseResolver.setRespData(data);
                return CompletableFuture.completedFuture(apiResponseResolver);
            }
        });
    }
    private CompletableFuture<ApiResponseResolver> generateOtp(ApiRequestResolver apiRequestResolver, String email){
        log.info("{} starting OTP generation for email: {}", apiRequestResolver.getLoggerString(), email);
        ApiResponseResolver apiResponseResolver = new ApiResponseResolver();
        ObjectNode data = mapper.createObjectNode();
        DataDynamicObject ddo = redisManager.getDdoData(GENERATE_OTP);
        String serviceUrl = redisManager.getServiceEndpoint(ddo.getService());
        String apiEndpoint = serviceUrl.concat(ddo.getApi());

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(apiEndpoint);
        uriBuilder.queryParam(EMAIL, email);
        HttpMethod method = Utils.getHttpMethod(ddo.getMethod());
        return wsUtils.makeWSCall(uriBuilder.toUriString(), null, new HashMap<>(), method,
                ddo.getConnectTimeout(), ddo.getReadTimeout(), ddo.getReturnClass()).thenApplyAsync(response -> {
            apiResponseResolver.setStatusCode(response.getHttpStatusCode());
            if(SUCCESS.equalsIgnoreCase(response.getStatus())){
                log.info("{} OTP generated for email: {}", apiRequestResolver.getLoggerString(), email);
                data.put(MESSAGE, "OTP generated successfully");
            } else {
                log.error("{} Error generating OTP for email: {}", apiRequestResolver.getLoggerString(), email);
                data.put(MESSAGE, "Error generating OTP");
            }
            apiResponseResolver.setRespData(data);
            return apiResponseResolver;
        });
    }
    private CompletableFuture<Boolean> validateOtp(ApiRequestResolver apiRequestResolver, String email, String otp){
        log.info("{} starting OTP validation for email: {}", apiRequestResolver.getLoggerString(), email);
        ApiResponseResolver apiResponseResolver = new ApiResponseResolver();
        DataDynamicObject ddo = redisManager.getDdoData(VALIDATE_OTP);
        String serviceUrl = redisManager.getServiceEndpoint(ddo.getService());
        String apiEndpoint = serviceUrl.concat(ddo.getApi());

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(apiEndpoint);
        uriBuilder.queryParam(EMAIL, email);
        uriBuilder.queryParam(OTP, otp);
        HttpMethod method = Utils.getHttpMethod(ddo.getMethod());
        return wsUtils.makeWSCall(uriBuilder.toUriString(), null, new HashMap<>(), method,
                ddo.getConnectTimeout(), ddo.getReadTimeout(), ddo.getReturnClass()).thenApplyAsync(response -> {
            apiResponseResolver.setStatusCode(response.getHttpStatusCode());
            if(SUCCESS.equalsIgnoreCase(response.getStatus())){
                log.info("{} OTP validated for email: {}", apiRequestResolver.getLoggerString(), email);
                return true;
            } else {
                log.error("{} Error validating OTP for email: {}", apiRequestResolver.getLoggerString(), email);
                return false;
            }
        });
    }
    private CompletableFuture<Boolean> createProfile(ApiRequestResolver apiRequestResolver, String profileType){
        log.info("{} Starting to create profile for request {}", apiRequestResolver.getLoggerString(), apiRequestResolver.getRequestBody());
        String ddoToGetProfileId = Utils.ddoCreateProfileByType(profileType);
        if(ddoToGetProfileId == null){
            log.error("{} Unsupported profile type for creating profile", apiRequestResolver.getLoggerString());
            return CompletableFuture.completedFuture(false);
        }
        DataDynamicObject ddo = redisManager.getDdoData(ddoToGetProfileId);
        //ensure in ddo config, api is kept as POST customers or merchants
        String serviceUrl = redisManager.getServiceEndpoint(ddo.getService());
        String apiEndpoint = serviceUrl.concat(ddo.getApi());
        HttpMethod method = Utils.getHttpMethod(ddo.getMethod());
        return wsUtils.makeWSCall(apiEndpoint, apiRequestResolver.getRequestBody(), new HashMap<>(), method,
                ddo.getConnectTimeout(), ddo.getReadTimeout(), ddo.getReturnClass()).thenApplyAsync(createResp -> {
            log.debug("{} profile create call completed with response {}", apiRequestResolver.getLoggerString(), createResp);
            if(createResp == null || FAILURE.equalsIgnoreCase(createResp.getStatus())){
                log.error("{} Error creating profile for request {}", apiRequestResolver.getLoggerString(), apiRequestResolver.getRequestBody());
                return false;
            } else {
                log.info("{} Profile created successfully for request {}", apiRequestResolver.getLoggerString(), apiRequestResolver.getRequestBody());
                return true;
            }
        });
    }
    private CompletableFuture<ApiResponseResolver> createProfileAndRetrieveId(ApiRequestResolver apiRequestResolver, String email, String profileType){
        log.info("Starting profile create flow for email: {} and profileType: {}", email, profileType);
        if(ADMIN.equalsIgnoreCase(profileType)){
            String adminUserId = redisManager.getHashValue(REDIS_FEATURE_FLAGS, ADMIN_USER_ID);
            log.info("{} Admin profile type has a hard coded userId : {}, hence no need to create profile",
                    apiRequestResolver.getLoggerString(), adminUserId);
            ApiResponseResolver apiResponseResolver = new ApiResponseResolver();
            ObjectNode data = mapper.createObjectNode();
            apiResponseResolver.setStatusCode(HttpStatus.OK); // 200 ok
            data.put(USER_ID, adminUserId);
            apiResponseResolver.setRespData(data);
            return CompletableFuture.completedFuture(apiResponseResolver);
        }
        ApiResponseResolver apiResponseResolver = new ApiResponseResolver();
        ObjectNode data = mapper.createObjectNode();
        return createProfile(apiRequestResolver, profileType).thenComposeAsync(profCreationResult -> {
            if(!profCreationResult){
                log.error("{} failure occurred in profile creation", apiRequestResolver.getLoggerString());
                apiResponseResolver.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR); // 500 internal server error
                data.put(MESSAGE, "Some error occurred while creating profile, please try again.");
                apiResponseResolver.setRespData(data);
                return CompletableFuture.completedFuture(apiResponseResolver);
            } else {
                log.info("{} profile created successfully for email: {}", apiRequestResolver.getLoggerString(), email);
                return fetchUserIdForEmail(apiRequestResolver, email, profileType).thenApplyAsync(userId -> {
                    if(StringUtils.isEmpty(userId)){
                        log.error("{} Error occurred while fetching userId for email: {} after profile creation", apiRequestResolver.getLoggerString(), email);
                        apiResponseResolver.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR); // 500 internal server error
                        data.put(MESSAGE, "Some error occurred while creating profile, please try again.");
                        apiResponseResolver.setRespData(data);
                        return apiResponseResolver;
                    }
                    log.info("{} retrieved user id email: {} after profile creation, so returning userId {}", apiRequestResolver.getLoggerString(), email, userId);
                    apiResponseResolver.setStatusCode(HttpStatus.OK); // 200 ok
                    data.put(USER_ID, userId);
                    apiResponseResolver.setRespData(data);
                    return apiResponseResolver;
                });
            }
        });
    }
    private CompletableFuture<String> fetchUserIdForEmail(ApiRequestResolver apiRequestResolver, String email, String profileType){
        log.info("{} fetching user id for email {} for profileType {}", apiRequestResolver.getLoggerString(), email, profileType);
        if(ADMIN.equalsIgnoreCase(profileType)){
            String adminUserId = redisManager.getHashValue(REDIS_FEATURE_FLAGS, ADMIN_USER_ID);
            log.info("{} Admin profile type has a hard coded userId : {}", apiRequestResolver.getLoggerString(), adminUserId);
            return CompletableFuture.completedFuture(adminUserId);
        }
        String ddoToGetProfileId = Utils.getDdoForFetchProfileIdByType(profileType);
        if(ddoToGetProfileId == null){
            log.error("{} Unsupported profile type for fetching user id for email", apiRequestResolver.getLoggerString());
            return CompletableFuture.completedFuture("");
        }
        DataDynamicObject ddo = redisManager.getDdoData(ddoToGetProfileId);
        //ensure in ddo config, api is kept as GET customers/email or merchants/email
        String serviceUrl = redisManager.getServiceEndpoint(ddo.getService());
        String apiEndpoint = serviceUrl.concat(ddo.getApi());
        apiEndpoint = apiEndpoint.concat(SLASH).concat(email);
        HttpMethod method = Utils.getHttpMethod(ddo.getMethod());
        return wsUtils.makeWSCall(apiEndpoint, null, new HashMap<>(), method, ddo.getConnectTimeout(),
                ddo.getReadTimeout(), ddo.getReturnClass()).thenApplyAsync(response -> {
            log.debug("{} profile fetchId call completed with response {}", apiRequestResolver.getLoggerString(), response);
            if(response==null || FAILURE.equalsIgnoreCase(response.getStatus())){
                log.info("{} No user found for email: {}", apiRequestResolver.getLoggerString(), email);
                return "";
            }else{
                log.info("{} User found for email: {} and it is : {}", apiRequestResolver.getLoggerString(), email, response.getData().get(MESSAGE).textValue());
                //in ws call we are returning against message.
                String userId = response.getData().get(MESSAGE).textValue();
                userId = userId.trim().replace("\"", "");
                log.debug("{} User after trim and replace is : {}", apiRequestResolver.getLoggerString(), userId);
                return userId;
            }
        });
    }

    private boolean checkIfValidAdminEmailId(String emailId){
        String adminEmailId = redisManager.getHashValue(REDIS_FEATURE_FLAGS, ADMIN_EMAIL_ID);
        if(adminEmailId==null || StringUtils.isEmpty(adminEmailId)){
            return false;
        }
        return adminEmailId.equalsIgnoreCase(emailId);
    }
}

package sg.edu.nus.iss.shopsmart_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
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

    public CompletableFuture<Boolean> generateOtp(ApiRequestResolver apiRequestResolver, String email){
        log.info("{} starting OTP generation for email: {}", apiRequestResolver.getLoggerString(), email);
        DataDynamicObject ddo = redisManager.getDdoData(GENERATE_OTP);
        String serviceUrl = redisManager.getServiceEndpoint(ddo.getService());
        String apiEndpoint = serviceUrl.concat(ddo.getApi());

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(apiEndpoint);
        uriBuilder.queryParam(EMAIL, email);
        HttpMethod method = Utils.getHttpMethod(ddo.getMethod());
        return wsUtils.makeWSCall(uriBuilder.toUriString(), null, new HashMap<>(), method,
                ddo.getConnectTimeout(), ddo.getReadTimeout(), ddo.getReturnClass()).thenApplyAsync(response -> {
            if(SUCCESS.equalsIgnoreCase(response.getStatus())){
                log.info("{} OTP generated for email: {}", apiRequestResolver.getLoggerString(), email);
                return true;
            } else {
                log.error("{} Error generating OTP for email: {}", apiRequestResolver.getLoggerString(), email);
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> validateOtp(ApiRequestResolver apiRequestResolver, String email, String otp){
        log.info("{} starting OTP validation for email: {}", apiRequestResolver.getLoggerString(), email);
        DataDynamicObject ddo = redisManager.getDdoData(VALIDATE_OTP);
        String serviceUrl = redisManager.getServiceEndpoint(ddo.getService());
        String apiEndpoint = serviceUrl.concat(ddo.getApi());

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(apiEndpoint);
        uriBuilder.queryParam(EMAIL, email);
        uriBuilder.queryParam(OTP, otp);
        HttpMethod method = Utils.getHttpMethod(ddo.getMethod());
        return wsUtils.makeWSCall(uriBuilder.toUriString(), null, new HashMap<>(), method,
                ddo.getConnectTimeout(), ddo.getReadTimeout(), ddo.getReturnClass()).thenApplyAsync(response -> {
            if(SUCCESS.equalsIgnoreCase(response.getStatus())){
                log.info("{} OTP validated for email: {}", apiRequestResolver.getLoggerString(), email);
                return true;
            } else {
                log.error("{} Error validating OTP for email: {}", apiRequestResolver.getLoggerString(), email);
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> createProfile(ApiRequestResolver apiRequestResolver, String profileType){
        log.info("{} Starting to create profile for request {}", apiRequestResolver.getLoggerString(), apiRequestResolver.getRequestBody());
        if(ADMIN.equalsIgnoreCase(profileType)){
            log.info("{} Admin profile type has a hard coded userId, hence no need to create profile, skipping to fetch profile",
                    apiRequestResolver.getLoggerString());
            return CompletableFuture.completedFuture(true);
        }
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

    public CompletableFuture<String> fetchUserIdForEmail(ApiRequestResolver apiRequestResolver, String email, String profileType){
        log.info("{} fetching user id for email {} for profileType {}", apiRequestResolver.getLoggerString(), email, profileType);
        if(ADMIN.equalsIgnoreCase(profileType) ){
            if(checkIfValidAdminEmailId(email)){
                String adminUserId = redisManager.getHashValue(REDIS_FEATURE_FLAGS, ADMIN_USER_ID);
                log.info("{} Admin profile type has a hard coded userId : {}", apiRequestResolver.getLoggerString(), adminUserId);
                return CompletableFuture.completedFuture(adminUserId);
            } else{
                log.error("{} Email {} is not valid for admin, not authorized", apiRequestResolver.getLoggerString(), email);
                return CompletableFuture.completedFuture("");
            }
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

    public boolean checkIfValidAdminEmailId(String emailId){
        String adminEmailId = redisManager.getHashValue(REDIS_FEATURE_FLAGS, ADMIN_EMAIL_ID);
        if(adminEmailId==null || StringUtils.isEmpty(adminEmailId)){
            return false;
        }
        return adminEmailId.equalsIgnoreCase(emailId);
    }
}

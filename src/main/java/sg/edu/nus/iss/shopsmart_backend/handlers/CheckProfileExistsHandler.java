package sg.edu.nus.iss.shopsmart_backend.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.model.ApiResponseResolver;
import sg.edu.nus.iss.shopsmart_backend.service.ProfileService;
import sg.edu.nus.iss.shopsmart_backend.utils.Constants;

import java.util.concurrent.CompletableFuture;

public class CheckProfileExistsHandler extends Constants implements Handler{
    private static final Logger log = LoggerFactory.getLogger(CheckProfileExistsHandler.class);
    private final ObjectMapper mapper = Json.mapper();

    private Handler nextHandler; // Next handler in the chain
    private final ProfileService profileService;

    public CheckProfileExistsHandler(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Override
    public void setNext(Handler nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public CompletableFuture<ApiResponseResolver> handle(ApiRequestResolver apiRequestResolver, String profileType) {
        log.info("{} starting checkProfileExistsHandler", apiRequestResolver.getLoggerString());
        ApiResponseResolver apiResponseResolver = new ApiResponseResolver();
        ObjectNode data = mapper.createObjectNode();
        String email = apiRequestResolver.getParamValue(EMAIL);
        if(StringUtils.isEmpty(email)){
            log.error("{} Email is empty in params", apiRequestResolver.getLoggerString());
            apiResponseResolver.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            data.put(MESSAGE, "Email is empty or not found in request for fetching user id");
            apiResponseResolver.setRespData(data);
            return CompletableFuture.completedFuture(apiResponseResolver);
        }
        return profileService.fetchUserIdForEmail(apiRequestResolver, email, profileType).thenComposeAsync(userId -> {
            String needsProfVal = apiRequestResolver.getParamValue(NEEDS_USER_PROF);
            boolean needsProfForReq = true;
            if(StringUtils.isNotEmpty(needsProfVal)){
                needsProfForReq = Boolean.parseBoolean(needsProfVal);
            }
            log.info("{} checkProfileExistsHandler for email {} completed with userId {} and needsProfForReq {}", apiRequestResolver.getLoggerString(), email, userId, needsProfForReq);
            if(StringUtils.isEmpty(userId)){
                if(!needsProfForReq && nextHandler!=null){
                    log.info("{} No userId found for email {} and needsProfForReq is false and found next link in chain so passing to next handler",
                            apiRequestResolver.getLoggerString(), email);
                    return nextHandler.handle(apiRequestResolver, profileType);
                }
                log.error("{} Error occurred while fetching userId for email: {} after profile creation", apiRequestResolver.getLoggerString(), email);
                apiResponseResolver.setStatusCode(HttpStatus.NOT_FOUND);
                data.put(MESSAGE, "No existing profile found for the email");
                apiResponseResolver.setRespData(data);
                return CompletableFuture.completedFuture(apiResponseResolver);
            }
            if(needsProfForReq && nextHandler!=null){
                log.info("{} User id found and needsProfForReq is true and found next link in chain so passing to next handler",
                        apiRequestResolver.getLoggerString());
                return nextHandler.handle(apiRequestResolver, profileType);
            }
            log.info("{} User id found and no next link in chain so returning response", apiRequestResolver.getLoggerString());
            apiResponseResolver.setStatusCode(HttpStatus.OK); // 200 ok
            data.put(USER_ID, userId);
            apiResponseResolver.setRespData(data);
            return CompletableFuture.completedFuture(apiResponseResolver);
        });
    }
}

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

public class FetchUserIdForEmailHandler extends Constants implements Handler{
    private static final Logger log = LoggerFactory.getLogger(FetchUserIdForEmailHandler.class);
    private final ObjectMapper mapper = Json.mapper();

    private Handler nextHandler; // Next handler in the chain
    private final ProfileService profileService;

    public FetchUserIdForEmailHandler(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Override
    public void setNext(Handler nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public CompletableFuture<ApiResponseResolver> handle(ApiRequestResolver apiRequestResolver, String profileType) {
        log.info("{} starting fetchUserIdForEmailHandler", apiRequestResolver.getLoggerString());
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
            log.info("{} fetchUserIdForEmailHandler for email {} completed with userId {}", apiRequestResolver.getLoggerString(), email, userId);
            if(StringUtils.isEmpty(userId)){
                log.error("{} Error occurred while fetching userId for email: {} after profile creation", apiRequestResolver.getLoggerString(), email);
                apiResponseResolver.setStatusCode(HttpStatus.NOT_FOUND);
                data.put(MESSAGE, "No existing profile found for the email");
                apiResponseResolver.setRespData(data);
                return CompletableFuture.completedFuture(apiResponseResolver);
            }
            if(nextHandler!=null){
                log.info("{} User id found and found next link in chain so passing to next handler",
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

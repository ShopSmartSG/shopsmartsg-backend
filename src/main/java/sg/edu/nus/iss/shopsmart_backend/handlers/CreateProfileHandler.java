package sg.edu.nus.iss.shopsmart_backend.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.model.ApiResponseResolver;
import sg.edu.nus.iss.shopsmart_backend.service.ProfileService;
import sg.edu.nus.iss.shopsmart_backend.utils.Constants;

import java.util.concurrent.CompletableFuture;

public class CreateProfileHandler extends Constants implements Handler {
    private static final Logger log = LoggerFactory.getLogger(CreateProfileHandler.class);
    private final ObjectMapper mapper = Json.mapper();

    private Handler nextHandler; // Next handler in the chain
    private final ProfileService profileService;

    public CreateProfileHandler(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Override
    public void setNext(Handler nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public CompletableFuture<ApiResponseResolver> handle(ApiRequestResolver apiRequestResolver, String profileType) {
        log.info("{} starting createProfileHandler", apiRequestResolver.getLoggerString());
        ApiResponseResolver apiResponseResolver = new ApiResponseResolver();
        ObjectNode data = mapper.createObjectNode();

        return profileService.createProfile(apiRequestResolver, profileType).thenComposeAsync(profileCreateResp -> {
            log.info("{} createProfile call completed with resp : {}", apiRequestResolver.getLoggerString(), profileCreateResp);
            if(!profileCreateResp){
                log.error("{} Profile creation failed for provided request", apiRequestResolver.getLoggerString());
                apiResponseResolver.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                data.put(MESSAGE, "Unable to create profile for the provided request");
                apiResponseResolver.setRespData(data);
                return CompletableFuture.completedFuture(apiResponseResolver);
            }
            if(nextHandler!=null){
                log.info("{} Profile creation completed CreateProfileHandler and found next link in chain so passing to next handler",
                        apiRequestResolver.getLoggerString());
                return nextHandler.handle(apiRequestResolver, profileType);
            }
            log.info("{} profile creation completed and no next link in chain so returning response", apiRequestResolver.getLoggerString());
            apiResponseResolver.setStatusCode(HttpStatus.OK); // 200 ok
            data.put(MESSAGE, "Profile created successfully for user");
            apiResponseResolver.setRespData(data);
            return CompletableFuture.completedFuture(apiResponseResolver);
        });
    }
}

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

public class GenerateOtpHandler extends Constants implements Handler{
    private static final Logger log = LoggerFactory.getLogger(GenerateOtpHandler.class);
    private final ObjectMapper mapper = Json.mapper();

    private Handler nextHandler; // Next handler in the chain
    private final ProfileService profileService;

    public GenerateOtpHandler(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Override
    public void setNext(Handler nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public CompletableFuture<ApiResponseResolver> handle(ApiRequestResolver apiRequestResolver, String profileType) {
        log.info("{} starting GenerateOtpHandler", apiRequestResolver.getLoggerString());
        ApiResponseResolver apiResponseResolver = new ApiResponseResolver();
        ObjectNode data = mapper.createObjectNode();

        String email = apiRequestResolver.getParamValue(EMAIL);
        if(StringUtils.isEmpty(email)){
            log.error("{} Email is empty in params", apiRequestResolver.getLoggerString());
            apiResponseResolver.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            data.put(MESSAGE, "Email is empty or not found in request for otp generation");
            apiResponseResolver.setRespData(data);
            return CompletableFuture.completedFuture(apiResponseResolver);
        }
        return profileService.generateOtp(apiRequestResolver, email).thenComposeAsync(otpGenerationResp->{
            log.info("{} otpGeneration call completed with resp {}", apiRequestResolver.getLoggerString(), otpGenerationResp);
            if(!otpGenerationResp){
                log.error("{} otp generation failed for provided email {}", apiRequestResolver.getLoggerString(), email);
                apiResponseResolver.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                data.put(MESSAGE, "Failure occurred in generation of otp for provided email");
                apiResponseResolver.setRespData(data);
                return CompletableFuture.completedFuture(apiResponseResolver);
            }
            if(nextHandler!=null){
                log.info("{} Otp generation completed GenerateOtpHandler and found next link in chain so passing to next handler",
                        apiRequestResolver.getLoggerString());
                return nextHandler.handle(apiRequestResolver, profileType);
            }
            log.info("{} otp generation completed and no next link in chain so returning response", apiRequestResolver.getLoggerString());
            apiResponseResolver.setStatusCode(HttpStatus.OK); // 200 ok
            data.put(MESSAGE, "Otp generation completed as expected");
            apiResponseResolver.setRespData(data);
            return CompletableFuture.completedFuture(apiResponseResolver);
        });
    }
}

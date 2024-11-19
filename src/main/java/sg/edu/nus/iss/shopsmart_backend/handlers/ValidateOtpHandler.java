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

public class ValidateOtpHandler extends Constants implements Handler{
    private static final Logger log = LoggerFactory.getLogger(ValidateOtpHandler.class);
    private final ObjectMapper mapper = Json.mapper();

    private Handler nextHandler; // Next handler in the chain
    private final ProfileService profileService;

    public ValidateOtpHandler(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Override
    public void setNext(Handler nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public CompletableFuture<ApiResponseResolver> handle(ApiRequestResolver apiRequestResolver, String profileType) {
        log.info("{} starting ValidateOtpHandler", apiRequestResolver.getLoggerString());
        ApiResponseResolver apiResponseResolver = new ApiResponseResolver();
        ObjectNode data = mapper.createObjectNode();

        String email = apiRequestResolver.getParamValue(EMAIL);
        String otp = apiRequestResolver.getParamValue(OTP);
        if(StringUtils.isEmpty(email) || StringUtils.isEmpty(otp)){
            log.error("{} Email or otp is empty in params", apiRequestResolver.getLoggerString());
            apiResponseResolver.setStatusCode(HttpStatus.NOT_FOUND);
            data.put(MESSAGE, "Email or otp is empty or not found in request for otp validation");
            apiResponseResolver.setRespData(data);
            return CompletableFuture.completedFuture(apiResponseResolver);
        }

        return profileService.validateOtp(apiRequestResolver, email, otp).thenComposeAsync(otpValidationResp -> {
            log.info("{} validateOtp call completed with resp : {}", apiRequestResolver.getLoggerString(), otpValidationResp);
            if(!otpValidationResp){
                log.error("{} otp validation failed for provided email {} and otp {}", apiRequestResolver.getLoggerString(), email, otp);
                apiResponseResolver.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                data.put(MESSAGE, "Failure occurred in validation of otp for provided request");
                apiResponseResolver.setRespData(data);
                return CompletableFuture.completedFuture(apiResponseResolver);
            }
            if(nextHandler!=null){
                log.info("{} Otp validation completed ValidateOtpHandler and found next link in chain so passing to next handler",
                        apiRequestResolver.getLoggerString());
                return nextHandler.handle(apiRequestResolver, profileType);
            }
            log.info("{} otp validation completed and no next link in chain so returning response", apiRequestResolver.getLoggerString());
            apiResponseResolver.setStatusCode(HttpStatus.OK); // 200 ok
            data.put(MESSAGE, "Otp validation completed as expected");
            apiResponseResolver.setRespData(data);
            return CompletableFuture.completedFuture(apiResponseResolver);
        });
    }
}

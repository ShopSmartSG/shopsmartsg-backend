package sg.edu.nus.iss.shopsmart_backend.chains;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.shopsmart_backend.handlers.FetchUserIdForEmailHandler;
import sg.edu.nus.iss.shopsmart_backend.handlers.Handler;
import sg.edu.nus.iss.shopsmart_backend.handlers.ValidateOtpHandler;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.model.ApiResponseResolver;
import sg.edu.nus.iss.shopsmart_backend.service.ProfileService;
import sg.edu.nus.iss.shopsmart_backend.utils.Constants;

import java.util.concurrent.CompletableFuture;

@Service
public class ValidateOtpAndLoginChain extends Constants implements Chain{
    private static final Logger log = LoggerFactory.getLogger(ValidateOtpAndLoginChain.class);
    private final ObjectMapper mapper = Json.mapper();

    private final Handler handlerChain;
    private final ProfileService profileService;

    @Autowired
    public ValidateOtpAndLoginChain(ProfileService profileService) {
        this.profileService = profileService;
        Handler validateOtpHandler = new ValidateOtpHandler(profileService);
        Handler fetchUserIdForEmailHandler = new FetchUserIdForEmailHandler(profileService);

        validateOtpHandler.setNext(fetchUserIdForEmailHandler);
        fetchUserIdForEmailHandler.setNext(null);

        this.handlerChain = validateOtpHandler;
    }

    @Override
    public CompletableFuture<ApiResponseResolver> handleRequest(ApiRequestResolver apiRequestResolver, String profileType){
        log.info("Starting ValidateOtpAndLoginChain, for apiRequestResolver : {}, and profileType {}", apiRequestResolver, profileType);
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
        apiRequestResolver.addParamValue(EMAIL, email);
        apiRequestResolver.addParamValue(OTP, otp);
        return handlerChain.handle(apiRequestResolver, profileType);
    }
}

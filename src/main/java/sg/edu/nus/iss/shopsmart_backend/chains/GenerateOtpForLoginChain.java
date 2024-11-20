package sg.edu.nus.iss.shopsmart_backend.chains;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.shopsmart_backend.handlers.CheckProfileExistsHandler;
import sg.edu.nus.iss.shopsmart_backend.handlers.GenerateOtpHandler;
import sg.edu.nus.iss.shopsmart_backend.handlers.Handler;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.model.ApiResponseResolver;
import sg.edu.nus.iss.shopsmart_backend.service.ProfileService;
import sg.edu.nus.iss.shopsmart_backend.utils.Constants;

import java.util.concurrent.CompletableFuture;

@Service
public class GenerateOtpForLoginChain extends Constants implements Chain{
    private static final Logger log = LoggerFactory.getLogger(GenerateOtpForLoginChain.class);
    private final ObjectMapper mapper = Json.mapper();

    @Getter
    private final Handler handlerChain;
    private final ProfileService profileService;

    @Autowired
    public GenerateOtpForLoginChain(ProfileService profileService) {
        this.profileService = profileService;
        Handler checkProfileExistsHandler = new CheckProfileExistsHandler(profileService);
        Handler generateOtpHandler = new GenerateOtpHandler(profileService);

        generateOtpHandler.setNext(null);
        checkProfileExistsHandler.setNext(generateOtpHandler);

        this.handlerChain = checkProfileExistsHandler;
    }

    @Override
    public CompletableFuture<ApiResponseResolver> handleRequest(ApiRequestResolver apiRequestResolver, String profileType) {
        log.info("Starting GenerateOtpForLoginChain, for apiRequestResolver : {}, and profileType {}", apiRequestResolver, profileType);
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
        if(ADMIN.equalsIgnoreCase(profileType) && !profileService.checkIfValidAdminEmailId(email)){
            log.info("{} Email {} is not valid for admin, not authorized", apiRequestResolver.getLoggerString(), email);
            apiResponseResolver.setStatusCode(HttpStatus.UNAUTHORIZED); // 401 unauthorized
            data.put(MESSAGE, "Email is not authorized for admin OTP generation");
            apiResponseResolver.setRespData(data);
            return CompletableFuture.completedFuture(apiResponseResolver);
        }
        apiRequestResolver.addParamValue(EMAIL, email);
        apiRequestResolver.addParamValue(NEEDS_USER_PROF, "true");
        return handlerChain.handle(apiRequestResolver, profileType);
    }
}

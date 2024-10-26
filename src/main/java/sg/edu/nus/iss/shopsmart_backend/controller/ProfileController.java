package sg.edu.nus.iss.shopsmart_backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.service.CommonService;
import sg.edu.nus.iss.shopsmart_backend.service.ProfileService;
import sg.edu.nus.iss.shopsmart_backend.utils.Constants;
import sg.edu.nus.iss.shopsmart_backend.utils.JsonUtils;
import sg.edu.nus.iss.shopsmart_backend.utils.Utils;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/profile")
@Tag(name = "Profile Login flows", description = "Handle login for customers and merchants profiles via APIs")
public class ProfileController extends Constants {
    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);
    private final ProfileService profileService;
    private final CommonService commonService;
    private final Utils utils;

    @Autowired
    public ProfileController(ProfileService profileService, CommonService commonService, Utils utils) {
        this.profileService = profileService;
        this.commonService = commonService;
        this.utils = utils;
    }

    @PostMapping("/register/generateOtp/{profileType}")
    public CompletableFuture<ResponseEntity<JsonNode>> generateOtpForRegister(@PathVariable String profileType, @RequestBody JsonNode requestBody,
                                       HttpServletRequest request, HttpServletResponse response){
        log.info("Starting flow for generate OTP for registration for profileType: {}", profileType);
        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, GENERATE_OTP, requestBody);
        long startTime = System.currentTimeMillis();
        return profileService.generateOtpForRegister(apiRequestResolver, profileType).thenApplyAsync(resp ->{
            log.info("{} Time taken to complete otp generation for registration is {} ms", apiRequestResolver.getLoggerString(),
                    (System.currentTimeMillis() - startTime));
            setRequiredCookies(apiRequestResolver, request, response);
            log.info("{} for sessionId {}, the following servlet response is being set {} for opt generation for registration",
                    apiRequestResolver.getLoggerString(), apiRequestResolver.getSessionId(), response.getHeaderNames());
            return new ResponseEntity<>(resp.getRespData(),Utils.createHeaders(), resp.getStatusCode());
        });
    }

    @PostMapping("/register/verifyOtp/{profileType}")
    public CompletableFuture<ResponseEntity<JsonNode>> verifyOtpForRegister(@PathVariable String profileType, @RequestBody JsonNode requestBody,
                                                                            HttpServletRequest request, HttpServletResponse response){
        log.info("Starting flow for validate OTP and createProfile for registration for profileType: {}", profileType);
        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, VALIDATE_OTP, requestBody);
        long startTime = System.currentTimeMillis();
        return profileService.validateOtpAndRegister(apiRequestResolver, profileType).thenApplyAsync(resp ->{
                log.info("{} Time taken to complete validate otp and create profile is {} ms", apiRequestResolver.getLoggerString(),
                        (System.currentTimeMillis() - startTime));
            String userId = JsonUtils.getText(resp.getRespData(), USER_ID);
            if(StringUtils.isNotEmpty(userId)){
                log.debug("{} found userId {} in response post user registration", apiRequestResolver.getLoggerString(), userId);
                apiRequestResolver.setUserId(userId);
                apiRequestResolver.setLoggedIn(true);
                commonService.updateUserIdInRedisInSessionData(apiRequestResolver);
            }
            setRequiredCookies(apiRequestResolver, request, response);
            log.info("{} for sessionId {}, the following servlet response is being set {} for validate otp and create profile",
                    apiRequestResolver.getLoggerString(), apiRequestResolver.getSessionId(), response.getHeaderNames());
            return new ResponseEntity<>(resp.getRespData(),Utils.createHeaders(), resp.getStatusCode());
        });
    }

    @PostMapping("/login/generateOtp/{profileType}")
    public CompletableFuture<ResponseEntity<JsonNode>> generateOtpForLogin(@PathVariable String profileType, @RequestBody JsonNode requestBody,
                                    HttpServletRequest request, HttpServletResponse response){
        log.info("Starting flow for generate OTP for login for profileType: {}", profileType);
        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, GENERATE_OTP, requestBody);
        long startTime = System.currentTimeMillis();
        return profileService.generateOtpForLogin(apiRequestResolver, profileType).thenApplyAsync(resp ->{
            log.info("{} Time taken to complete otp generation for login is {} ms", apiRequestResolver.getLoggerString(),
                    (System.currentTimeMillis() - startTime));
            setRequiredCookies(apiRequestResolver, request, response);
            log.info("{} for sessionId {}, the following servlet response is being set {} for opt generation for login",
                    apiRequestResolver.getLoggerString(), apiRequestResolver.getSessionId(), response.getHeaderNames());
            return new ResponseEntity<>(resp.getRespData(),Utils.createHeaders(), resp.getStatusCode());
        });
    }

    @PostMapping("/login/verifyOtp/{profileType}")
    public CompletableFuture<ResponseEntity<JsonNode>> verifyOtpForLogin(@PathVariable String profileType, @RequestBody JsonNode requestBody,
                                  HttpServletRequest request, HttpServletResponse response){
        log.info("Starting flow for validate OTP and createProfile for login for profileType: {}", profileType);
        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, VALIDATE_OTP, requestBody);
        long startTime = System.currentTimeMillis();
        return profileService.validateOtpAndLogin(apiRequestResolver, profileType).thenApplyAsync(resp ->{
            log.info("{} Time taken to complete validate otp and fetch userId is {} ms", apiRequestResolver.getLoggerString(),
                    (System.currentTimeMillis() - startTime));
            String userId = JsonUtils.getText(resp.getRespData(), USER_ID);
            if(StringUtils.isNotEmpty(userId)){
                log.debug("{} found userId {} in response post user login", apiRequestResolver.getLoggerString(), userId);
                apiRequestResolver.setUserId(userId);
                apiRequestResolver.setLoggedIn(true);
                commonService.updateUserIdInRedisInSessionData(apiRequestResolver);
            }
            setRequiredCookies(apiRequestResolver, request, response);
            log.info("{} for sessionId {}, the following servlet response is being set {} for validate otp and fetch userId",
                    apiRequestResolver.getLoggerString(), apiRequestResolver.getSessionId(), response.getHeaderNames());
            return new ResponseEntity<>(resp.getRespData(),Utils.createHeaders(), resp.getStatusCode());
        });
    }

    private void setRequiredCookies(ApiRequestResolver apiRequestResolver, HttpServletRequest request,
                                    HttpServletResponse response) {
        log.info("{} starting to set required session and user cookies for profile login flows.", apiRequestResolver.getLoggerString());
        utils.setSessionAndCookieDataForSession(apiRequestResolver, request, response);
        utils.setUserIdCookieNeededOrRemove(apiRequestResolver, request, response);
    }
}

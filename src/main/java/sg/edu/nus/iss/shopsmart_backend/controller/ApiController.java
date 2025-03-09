package sg.edu.nus.iss.shopsmart_backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.*;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.model.ApiResponseResolver;
import sg.edu.nus.iss.shopsmart_backend.service.ApiService;
import sg.edu.nus.iss.shopsmart_backend.service.CommonService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sg.edu.nus.iss.shopsmart_backend.utils.Constants;
import sg.edu.nus.iss.shopsmart_backend.utils.Utils;

import java.util.concurrent.CompletableFuture;


@RestController
@RequestMapping("/api")
@Tag(name = "API", description = "Handle all API calls to backend flows in a generic way.")
public class ApiController extends Constants {
    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    private final ApiService apiService;
    private final CommonService commonService;
    private final Utils utils;

    @Autowired
    public ApiController(ApiService apiService, CommonService commonService, Utils utils) {
        this.apiService = apiService;
        this.commonService = commonService;
        this.utils = utils;
    }


    //!!!!!! this is important, in the details part see if ApiRequestResolver is present and if yes then use that with updated api-key from actual request

    private ApiRequestResolver getApiRequestResolver(Authentication authentication, HttpServletRequest request, String apiKey, JsonNode requestBody) {
        ApiRequestResolver apiRequestResolver;
        if(authentication == null || authentication.getDetails() == null){
            log.info("No authentication or auth details found in request for apiKey {}", apiKey);
            apiRequestResolver = commonService.createApiResolverRequest(request, apiKey, requestBody);
            apiRequestResolver.setLoggedIn(false);
            apiRequestResolver.setUserId(null);
            return apiRequestResolver;
        }
        log.info("Authentication and auth details found in request for apiKey {}", apiKey);
        log.debug("Authentication details found in request for apiKey {} is {}", apiKey, authentication.getDetails());
        apiRequestResolver = (ApiRequestResolver) authentication.getDetails();
        apiRequestResolver.setApiKey(apiKey);
        apiRequestResolver.setRequestBody(requestBody);

        String userIdByPrinciple = (String) authentication.getPrincipal();
        apiRequestResolver.setUserId(userIdByPrinciple);
        if(userIdByPrinciple==null || userIdByPrinciple.isEmpty()){
            apiRequestResolver.setLoggedIn(false);
        }else{
            apiRequestResolver.setLoggedIn(true);
        }
        return apiRequestResolver;
    }

    @GetMapping("/{api-key}/**")
    public ResponseEntity<JsonNode> handleGetRequest(@CurrentSecurityContext(expression = "authentication") Authentication authentication,
                                                                        @PathVariable(name = "api-key") String apiKey,
                                                                        HttpServletRequest request, HttpServletResponse response) throws Exception{
        HttpHeaders headers = Utils.createHeaders(request);
        log.info("Handling GET request for API: {}", apiKey);
        ApiRequestResolver apiRequestResolver = getApiRequestResolver(authentication, request, apiKey, null);
        long startTime = System.currentTimeMillis();
        ApiResponseResolver resolvedResp = apiService.processApiRequest(apiRequestResolver).get();
        log.info("{} Time taken to complete GET api request {} is {} ms", apiRequestResolver.getLoggerString(),
                apiKey, (System.currentTimeMillis() - startTime));
        setRequiredCookies(apiRequestResolver, request, response);
        log.info("{} for sessionId {}, the following servlet response is being set {} for GET request",
                apiRequestResolver.getLoggerString(), apiRequestResolver.getSessionId(), response.getHeaderNames());
        return new ResponseEntity<>(resolvedResp.getRespData(), headers, resolvedResp.getStatusCode());
//        return apiService.processApiRequest(apiRequestResolver).thenApplyAsync(resolvedResp -> {
//                    log.info("{} Time taken to complete GET api request {} is {} ms", apiRequestResolver.getLoggerString(),
//                            apiKey, (System.currentTimeMillis() - startTime));
//                    setRequiredCookies(apiRequestResolver, request, response);
//                    log.info("{} for sessionId {}, the following servlet response is being set {} for GET request",
//                            apiRequestResolver.getLoggerString(), apiRequestResolver.getSessionId(), response.getHeaderNames());
//                    log.debug("Response data for GET request is {} and code {}", resolvedResp.getRespData(), resolvedResp.getStatusCode());
//                    return new ResponseEntity<>(resolvedResp.getRespData(), headers, resolvedResp.getStatusCode());
//                });
    }

    @PostMapping("/{api-key}/**")
    public ResponseEntity<JsonNode> handlePostRequest(@AuthenticationPrincipal Authentication authentication,
                                                                         @PathVariable(name = "api-key") String apiKey, @RequestBody JsonNode requestBody,
                                                                         HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("Handling POST request for API: {}", apiKey);
        HttpHeaders headers = Utils.createHeaders(request);
//        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, apiKey, requestBody);
        ApiRequestResolver apiRequestResolver = getApiRequestResolver(authentication, request, apiKey, requestBody);
        //perform jwt validation check here
        long startTime = System.currentTimeMillis();
        ApiResponseResolver resolvedResp = apiService.processApiRequest(apiRequestResolver).get();
        log.info("{} Time taken to complete POST api request {} is {} ms", apiRequestResolver.getLoggerString(),
                apiKey, (System.currentTimeMillis() - startTime));
        setRequiredCookies(apiRequestResolver, request, response);
        log.info("{} for sessionId {}, the following servlet response is being set {} for POST request",
                apiRequestResolver.getLoggerString(), apiRequestResolver.getSessionId(), response.getHeaderNames());
        return new ResponseEntity<>(resolvedResp.getRespData(),headers, resolvedResp.getStatusCode());
//        return apiService.processApiRequest(apiRequestResolver)
//                .thenApply(resolvedResp -> {
//                    log.info("{} Time taken to complete POST api request {} is {} ms", apiRequestResolver.getLoggerString(),
//                            apiKey, (System.currentTimeMillis() - startTime));
//                    setRequiredCookies(apiRequestResolver, request, response);
//                    log.info("{} for sessionId {}, the following servlet response is being set {} for POST request",
//                            apiRequestResolver.getLoggerString(), apiRequestResolver.getSessionId(), response.getHeaderNames());
//                    return new ResponseEntity<>(resolvedResp.getRespData(),headers, resolvedResp.getStatusCode());
//                });
    }

    @PutMapping("/{api-key}/**")
    public ResponseEntity<JsonNode> handlePutRequest(@AuthenticationPrincipal Authentication authentication,
                                                                        @PathVariable(name = "api-key") String apiKey, @RequestBody JsonNode requestBody,
                                                                        HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("Handling PUT request for API: {}", apiKey);
        HttpHeaders headers = Utils.createHeaders(request);
//        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, apiKey, requestBody);
        ApiRequestResolver apiRequestResolver = getApiRequestResolver(authentication, request, apiKey, requestBody);
        //perform jwt validation check here
        long startTime = System.currentTimeMillis();
        ApiResponseResolver resolvedResp = apiService.processApiRequest(apiRequestResolver).get();
        log.info("{} Time taken to complete PUT api request {} is {} ms", apiRequestResolver.getLoggerString(),
                apiKey, (System.currentTimeMillis() - startTime));
        setRequiredCookies(apiRequestResolver, request, response);
        log.info("{} for sessionId {}, the following servlet response is being set {} for PUT request",
                apiRequestResolver.getLoggerString(), apiRequestResolver.getSessionId(), response.getHeaderNames());
        return new ResponseEntity<>(resolvedResp.getRespData(),headers,resolvedResp.getStatusCode());
//        return apiService.processApiRequest(apiRequestResolver)
//                .thenApply(resolvedResp -> {
//                    log.info("{} Time taken to complete PUT api request {} is {} ms", apiRequestResolver.getLoggerString(),
//                            apiKey, (System.currentTimeMillis() - startTime));
//                    setRequiredCookies(apiRequestResolver, request, response);
//                    log.info("{} for sessionId {}, the following servlet response is being set {} for PUT request",
//                            apiRequestResolver.getLoggerString(), apiRequestResolver.getSessionId(), response.getHeaderNames());
//                    return new ResponseEntity<>(resolvedResp.getRespData(),headers,resolvedResp.getStatusCode());
//                });
    }

    @PatchMapping("/{api-key}/**")
    public ResponseEntity<JsonNode> handlePatchRequest(@AuthenticationPrincipal Authentication authentication,
                                                                          @PathVariable(name = "api-key") String apiKey, @RequestBody JsonNode requestBody,
                                                                          HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("Handling PATCH request for API: {}", apiKey);
        HttpHeaders headers = Utils.createHeaders(request);
//        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, apiKey, requestBody);
        ApiRequestResolver apiRequestResolver = getApiRequestResolver(authentication, request, apiKey, requestBody);
        //perform jwt validation check here
        long startTime = System.currentTimeMillis();
        ApiResponseResolver resolvedResp = apiService.processApiRequest(apiRequestResolver).get();
        log.info("{} Time taken to complete PATCH api request {} is {} ms", apiRequestResolver.getLoggerString(),
                apiKey, (System.currentTimeMillis() - startTime));
        setRequiredCookies(apiRequestResolver, request, response);
        log.info("{} for sessionId {}, the following servlet response is being set {} for PATCH request",
                apiRequestResolver.getLoggerString(), apiRequestResolver.getSessionId(), response.getHeaderNames());
        return new ResponseEntity<>(resolvedResp.getRespData(), headers,resolvedResp.getStatusCode());
//        return apiService.processApiRequest(apiRequestResolver)
//                .thenApply(resolvedResp -> {
//                    log.info("{} Time taken to complete PATCH api request {} is {} ms", apiRequestResolver.getLoggerString(),
//                            apiKey, (System.currentTimeMillis() - startTime));
//                    setRequiredCookies(apiRequestResolver, request, response);
//                    log.info("{} for sessionId {}, the following servlet response is being set {} for PATCH request",
//                            apiRequestResolver.getLoggerString(), apiRequestResolver.getSessionId(), response.getHeaderNames());
//                    return new ResponseEntity<>(resolvedResp.getRespData(), headers,resolvedResp.getStatusCode());
//                });
    }

    @DeleteMapping("/{api-key}/**")
    public ResponseEntity<JsonNode> handleDeleteRequest(@AuthenticationPrincipal Authentication authentication,
                                                                           @PathVariable(name = "api-key") String apiKey, HttpServletRequest request,
                                                                           HttpServletResponse response) throws Exception {
        log.info("Handling DELETE request for API: {}", apiKey);
        HttpHeaders headers = Utils.createHeaders(request);
//        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, apiKey, null);
        ApiRequestResolver apiRequestResolver = getApiRequestResolver(authentication, request, apiKey, null);
        //perform jwt validation check here
        long startTime = System.currentTimeMillis();
        ApiResponseResolver resolvedResp = apiService.processApiRequest(apiRequestResolver).get();
        log.info("{} Time taken to complete DELETE api request {} is {} ms", apiRequestResolver.getLoggerString(),
                apiKey, (System.currentTimeMillis() - startTime));
        setRequiredCookies(apiRequestResolver, request, response);
        log.info("{} for sessionId {}, the following servlet response is being set {} for DELETE request",
                apiRequestResolver.getLoggerString(), apiRequestResolver.getSessionId(), response.getHeaderNames());
        return new ResponseEntity<>(resolvedResp.getRespData(), headers,resolvedResp.getStatusCode());
//        return apiService.processApiRequest(apiRequestResolver)
//                .thenApply(resolvedResp -> {
//                    log.info("{} Time taken to complete DELETE api request {} is {} ms", apiRequestResolver.getLoggerString(),
//                            apiKey, (System.currentTimeMillis() - startTime));
//                    setRequiredCookies(apiRequestResolver, request, response);
//                    log.info("{} for sessionId {}, the following servlet response is being set {} for DELETE request",
//                            apiRequestResolver.getLoggerString(), apiRequestResolver.getSessionId(), response.getHeaderNames());
//                    return new ResponseEntity<>(resolvedResp.getRespData(), headers,resolvedResp.getStatusCode());
//                });
    }

    private void setRequiredCookies(ApiRequestResolver apiRequestResolver, HttpServletRequest request,
            HttpServletResponse response) {
        log.info("{} starting to set required session and user cookies for general api flows.", apiRequestResolver.getLoggerString());
        utils.setSessionAndCookieDataForSession(apiRequestResolver, request, response);
//        utils.setUserIdCookieNeededOrRemove(apiRequestResolver, request, response);
    }
}
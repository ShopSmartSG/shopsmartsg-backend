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
import org.springframework.web.bind.annotation.*;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
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

    @GetMapping("/{api-key}/**")
    public CompletableFuture<ResponseEntity<JsonNode>> handleGetRequest(@PathVariable(name = "api-key") String apiKey,
            HttpServletRequest request, HttpServletResponse response) {
        HttpHeaders headers = Utils.createHeaders();
        log.info("Handling GET request for API: {}", apiKey);
        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, apiKey, null);
        //perform jwt validation check here
        long startTime = System.currentTimeMillis();
        return apiService.processApiRequest(apiRequestResolver)
                .thenApply(resolvedResp -> {
                    log.info("{} Time taken to complete GET api request {} is {} ms", apiRequestResolver.getLoggerString(),
                            apiKey, (System.currentTimeMillis() - startTime));
                    setRequiredCookies(apiRequestResolver, request, response);
                    log.info("{} for sessionId {}, the following servlet response is being set {} for GET request",
                            apiRequestResolver.getLoggerString(), apiRequestResolver.getSessionId(), response.getHeaderNames());
                    return new ResponseEntity<>(resolvedResp.getRespData(), headers, resolvedResp.getStatusCode());
                });
    }

    @PostMapping("/{api-key}/**")
    public CompletableFuture<ResponseEntity<JsonNode>> handlePostRequest(@PathVariable(name = "api-key") String apiKey,
            @RequestBody JsonNode requestBody, HttpServletRequest request, HttpServletResponse response) {
        log.info("Handling POST request for API: {}", apiKey);
        HttpHeaders headers = Utils.createHeaders();
        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, apiKey, requestBody);
        //perform jwt validation check here
        long startTime = System.currentTimeMillis();
        return apiService.processApiRequest(apiRequestResolver)
                .thenApply(resolvedResp -> {
                    log.info("{} Time taken to complete POST api request {} is {} ms", apiRequestResolver.getLoggerString(),
                            apiKey, (System.currentTimeMillis() - startTime));
                    setRequiredCookies(apiRequestResolver, request, response);
                    log.info("{} for sessionId {}, the following servlet response is being set {} for POST request",
                            apiRequestResolver.getLoggerString(), apiRequestResolver.getSessionId(), response.getHeaderNames());
                    return new ResponseEntity<>(resolvedResp.getRespData(),headers, resolvedResp.getStatusCode());
                });
    }

    @PutMapping("/{api-key}/**")
    public CompletableFuture<ResponseEntity<JsonNode>> handlePutRequest(@PathVariable(name = "api-key") String apiKey,
            @RequestBody JsonNode requestBody, HttpServletRequest request, HttpServletResponse response) {
        log.info("Handling PUT request for API: {}", apiKey);
        HttpHeaders headers = Utils.createHeaders();
        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, apiKey, requestBody);
        //perform jwt validation check here
        long startTime = System.currentTimeMillis();
        return apiService.processApiRequest(apiRequestResolver)
                .thenApply(resolvedResp -> {
                    log.info("{} Time taken to complete PUT api request {} is {} ms", apiRequestResolver.getLoggerString(),
                            apiKey, (System.currentTimeMillis() - startTime));
                    setRequiredCookies(apiRequestResolver, request, response);
                    log.info("{} for sessionId {}, the following servlet response is being set {} for PUT request",
                            apiRequestResolver.getLoggerString(), apiRequestResolver.getSessionId(), response.getHeaderNames());
                    return new ResponseEntity<>(resolvedResp.getRespData(),headers,resolvedResp.getStatusCode());
                });
    }

    @PatchMapping("/{api-key}/**")
    public CompletableFuture<ResponseEntity<JsonNode>> handlePatchRequest(@PathVariable(name = "api-key") String apiKey,
            @RequestBody JsonNode requestBody, HttpServletRequest request, HttpServletResponse response) {
        log.info("Handling PATCH request for API: {}", apiKey);
        HttpHeaders headers = Utils.createHeaders();
        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, apiKey, requestBody);
        //perform jwt validation check here
        long startTime = System.currentTimeMillis();
        return apiService.processApiRequest(apiRequestResolver)
                .thenApply(resolvedResp -> {
                    log.info("{} Time taken to complete PATCH api request {} is {} ms", apiRequestResolver.getLoggerString(),
                            apiKey, (System.currentTimeMillis() - startTime));
                    setRequiredCookies(apiRequestResolver, request, response);
                    log.info("{} for sessionId {}, the following servlet response is being set {} for PATCH request",
                            apiRequestResolver.getLoggerString(), apiRequestResolver.getSessionId(), response.getHeaderNames());
                    return new ResponseEntity<>(resolvedResp.getRespData(), headers,resolvedResp.getStatusCode());
                });
    }

    @DeleteMapping("/{api-key}/**")
    public CompletableFuture<ResponseEntity<JsonNode>> handleDeleteRequest(@PathVariable(name = "api-key") String apiKey,
            HttpServletRequest request, HttpServletResponse response) {
        log.info("Handling DELETE request for API: {}", apiKey);
        HttpHeaders headers = Utils.createHeaders();
        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, apiKey, null);
        //perform jwt validation check here
        long startTime = System.currentTimeMillis();
        return apiService.processApiRequest(apiRequestResolver)
                .thenApply(resolvedResp -> {
                    log.info("{} Time taken to complete DELETE api request {} is {} ms", apiRequestResolver.getLoggerString(),
                            apiKey, (System.currentTimeMillis() - startTime));
                    setRequiredCookies(apiRequestResolver, request, response);
                    log.info("{} for sessionId {}, the following servlet response is being set {} for DELETE request",
                            apiRequestResolver.getLoggerString(), apiRequestResolver.getSessionId(), response.getHeaderNames());
                    return new ResponseEntity<>(resolvedResp.getRespData(), headers,resolvedResp.getStatusCode());
                });
    }

    private void setRequiredCookies(ApiRequestResolver apiRequestResolver, HttpServletRequest request,
            HttpServletResponse response) {
        log.info("{} starting to set required session and user cookies for general api flows.", apiRequestResolver.getLoggerString());
        utils.setSessionAndCookieDataForSession(apiRequestResolver, request, response);
        utils.setUserIdCookieNeededOrRemove(apiRequestResolver, request, response);
    }
}
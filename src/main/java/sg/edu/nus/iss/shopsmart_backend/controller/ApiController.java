package sg.edu.nus.iss.shopsmart_backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.model.Response;
import sg.edu.nus.iss.shopsmart_backend.service.ApiService;
import sg.edu.nus.iss.shopsmart_backend.service.CommonService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.concurrent.CompletableFuture;


@RestController
@RequestMapping("/api")
@Tag(name = "API", description = "Handle all API calls to backend flows in a generic way.")
public class ApiController {
    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    private final ApiService apiService;
    private final CommonService commonService;

    @Autowired
    public ApiController(ApiService apiService, CommonService commonService) {
        this.apiService = apiService;
        this.commonService = commonService;
    }

    @GetMapping("/{apiKey}/**")
    public CompletableFuture<ResponseEntity<JsonNode>> handleGetRequest(
            @PathVariable String apiKey,
            HttpServletRequest request,
            HttpServletResponse response) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range");
        headers.add("Access-Control-Expose-Headers", "Content-Length,Content-Range");
        log.info("Handling GET request for API: {}", apiKey);
        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, apiKey, null);
        //perform jwt validation check here
        long startTime = System.currentTimeMillis();
        return apiService.processApiRequest(apiRequestResolver)
                .thenApply(resolvedResp -> {
                    log.info("{} Time taken to complete GET api request {} is {} ms", apiRequestResolver.getLoggerString(),
                            apiKey, (System.currentTimeMillis() - startTime));
                    setSessionAndCookie(response, apiRequestResolver.getSessionId());
                    return new ResponseEntity<>(resolvedResp.getRespData(), headers, resolvedResp.getStatusCode());
                }).thenApplyAsync(resp -> {
                    log.info("Response: {}", resp);
                    return resp;
                });
    }

    @PostMapping("/{apiKey}/**")
    public CompletableFuture<ResponseEntity<JsonNode>> handlePostRequest(
            @PathVariable String apiKey,
            @RequestBody JsonNode requestBody,
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("Handling POST request for API: {}", apiKey);
        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, apiKey, requestBody);
        //perform jwt validation check here
        long startTime = System.currentTimeMillis();
        return apiService.processApiRequest(apiRequestResolver)
                .thenApply(resolvedResp -> {
                    log.info("{} Time taken to complete POST api request {} is {} ms", apiRequestResolver.getLoggerString(),
                            apiKey, (System.currentTimeMillis() - startTime));
                    setSessionAndCookie(response, apiRequestResolver.getSessionId());
                    return new ResponseEntity<>(resolvedResp.getRespData(), resolvedResp.getStatusCode());
                });
    }

    @PutMapping("/{apiKey}/**")
    public CompletableFuture<ResponseEntity<JsonNode>> handlePutRequest(
            @PathVariable String apiKey,
            @RequestBody JsonNode requestBody,
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("Handling PUT request for API: {}", apiKey);
        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, apiKey, requestBody);
        //perform jwt validation check here
        long startTime = System.currentTimeMillis();
        return apiService.processApiRequest(apiRequestResolver)
                .thenApply(resolvedResp -> {
                    log.info("{} Time taken to complete PUT api request {} is {} ms", apiRequestResolver.getLoggerString(),
                            apiKey, (System.currentTimeMillis() - startTime));
                    setSessionAndCookie(response, apiRequestResolver.getSessionId());
                    return new ResponseEntity<>(resolvedResp.getRespData(), resolvedResp.getStatusCode());
                });
    }

    @PatchMapping("/{apiKey}/**")
    public CompletableFuture<ResponseEntity<JsonNode>> handlePatchRequest(
            @PathVariable String apiKey,
            @RequestBody JsonNode requestBody,
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("Handling PATCH request for API: {}", apiKey);
        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, apiKey, requestBody);
        //perform jwt validation check here
        long startTime = System.currentTimeMillis();
        return apiService.processApiRequest(apiRequestResolver)
                .thenApply(resolvedResp -> {
                    log.info("{} Time taken to complete PATCH api request {} is {} ms", apiRequestResolver.getLoggerString(),
                            apiKey, (System.currentTimeMillis() - startTime));
                    setSessionAndCookie(response, apiRequestResolver.getSessionId());
                    return new ResponseEntity<>(resolvedResp.getRespData(), resolvedResp.getStatusCode());
                });
    }

    @DeleteMapping("/{apiKey}/**")
    public CompletableFuture<ResponseEntity<JsonNode>> handleDeleteRequest(
            @PathVariable String apiKey,
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("Handling DELETE request for API: {}", apiKey);
        ApiRequestResolver apiRequestResolver = commonService.createApiResolverRequest(request, apiKey, null);
        //perform jwt validation check here
        long startTime = System.currentTimeMillis();
        return apiService.processApiRequest(apiRequestResolver)
                .thenApply(resolvedResp -> {
                    log.info("{} Time taken to complete DELETE api request {} is {} ms", apiRequestResolver.getLoggerString(),
                            apiKey, (System.currentTimeMillis() - startTime));
                    setSessionAndCookie(response, apiRequestResolver.getSessionId());
                    return new ResponseEntity<>(resolvedResp.getRespData(), resolvedResp.getStatusCode());
                });
    }

    private void setSessionAndCookie(HttpServletResponse response, String sessionId) {
        // Set session ID in response cookies
        Cookie sessionCookie = new Cookie("SESSION", sessionId);
        sessionCookie.setPath("/");
        sessionCookie.setHttpOnly(true);
        response.addCookie(sessionCookie);

        // Set session data in response headers (if needed)
        response.setHeader("Set-Cookie", "SESSION=" + sessionId + "; Path=/; HttpOnly");
    }
}
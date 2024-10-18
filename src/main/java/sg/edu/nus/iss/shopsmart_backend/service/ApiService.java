package sg.edu.nus.iss.shopsmart_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.model.ApiResponseResolver;
import sg.edu.nus.iss.shopsmart_backend.model.Response;
import sg.edu.nus.iss.shopsmart_backend.utils.ApplicationConstants;
import sg.edu.nus.iss.shopsmart_backend.utils.WSUtils;

import java.util.concurrent.CompletableFuture;

@Service
public class ApiService extends ApplicationConstants {
    private static final Logger log = LoggerFactory.getLogger(ApiService.class);
    private final ObjectMapper mapper = Json.mapper();

    private final WSUtils wsUtils;

    @Autowired
    public ApiService(WSUtils wsUtils) {
        this.wsUtils = wsUtils;
    }

    public CompletableFuture<ApiResponseResolver> processApiRequest(ApiRequestResolver apiRequestResolver){
        log.info("{} Processing API request for key: {}", apiRequestResolver.getLoggerString(), apiRequestResolver.getApiKey());
        log.debug("{} Processing API request for key: {} with request object: {}, headers {} and queryParams {}", apiRequestResolver.getLoggerString(), apiRequestResolver.getApiKey(),
                apiRequestResolver.getRequestBody(), apiRequestResolver.getHeaders(), apiRequestResolver.getQueryParams());
        //first perform JWT validation or have validated data passed in apiRequestResolver.
        //then if needed reconstruct the request object
        JsonNode requestBody = addCommonFieldsToRequest(apiRequestResolver);
        return wsUtils.makeWSCall(apiRequestResolver.getApiKey(), requestBody,
                apiRequestResolver.getHeaders(), apiRequestResolver.getQueryParams()).thenApplyAsync(response -> {
            log.debug("{} Received response for API key: {}", apiRequestResolver.getLoggerString(), apiRequestResolver.getApiKey());
            ApiResponseResolver apiResponseResolver = new ApiResponseResolver();
            apiResponseResolver.setStatusCode(response.getHttpStatusCode());
            if(response.getHttpStatusCode() == HttpStatus.OK){
                log.info("{} Success :: For the API key: {} received response {}", apiRequestResolver.getLoggerString(),
                        apiRequestResolver.getApiKey(), response);
                JsonNode respData = response.getData();
                //add required userId and jwtToken to response.
                apiResponseResolver.setRespData(respData);
            } else {
                log.info("{} Failure :: For the API key: {} received response {}", apiRequestResolver.getLoggerString(),
                        apiRequestResolver.getApiKey(), response);
                apiResponseResolver.setRespData(response.getData());
            }
            return apiResponseResolver;
        });
    }

    private JsonNode addCommonFieldsToRequest(ApiRequestResolver apiRequestResolver){
        ObjectNode requestBody = (ObjectNode) apiRequestResolver.getRequestBody();
        ObjectNode commonFields = mapper.createObjectNode();
        commonFields.put(USER_ID, apiRequestResolver.getUserId());
        commonFields.put(JWT_TOKEN, apiRequestResolver.getJwtToken());
        commonFields.put(IP_ADDRESS, apiRequestResolver.getIpAddress());
        requestBody.set(COMMON, commonFields);
        return requestBody;
    }
}

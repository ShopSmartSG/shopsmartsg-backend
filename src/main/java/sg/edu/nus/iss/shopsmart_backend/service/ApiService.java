package sg.edu.nus.iss.shopsmart_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.model.ApiResponseResolver;
import sg.edu.nus.iss.shopsmart_backend.model.DataDynamicObject;
import sg.edu.nus.iss.shopsmart_backend.utils.Constants;
import sg.edu.nus.iss.shopsmart_backend.utils.RedisManager;
import sg.edu.nus.iss.shopsmart_backend.utils.Utils;
import sg.edu.nus.iss.shopsmart_backend.utils.WSUtils;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class ApiService extends Constants {
    private static final Logger log = LoggerFactory.getLogger(ApiService.class);
    private final ObjectMapper mapper = Json.mapper();

    private final WSUtils wsUtils;
    private final RedisManager redisManager;

    @Autowired
    public ApiService(WSUtils wsUtils, RedisManager redisManager) {
        this.wsUtils = wsUtils;
        this.redisManager = redisManager;
    }

    public CompletableFuture<ApiResponseResolver> processApiRequest(ApiRequestResolver apiRequestResolver){
        log.info("{} Processing API request for key: {}", apiRequestResolver.getLoggerString(), apiRequestResolver.getApiKey());
        log.debug("{} Processing API request for key: {} with request object: {}, headers {} and queryParams {}", apiRequestResolver.getLoggerString(), apiRequestResolver.getApiKey(),
                apiRequestResolver.getRequestBody(), apiRequestResolver.getHeaders(), apiRequestResolver.getQueryParams());
        ApiResponseResolver apiResponseResolver = new ApiResponseResolver();
        String apiKey = apiRequestResolver.getApiKey();
        DataDynamicObject ddo = redisManager.getDdoData(apiKey);
        if (ddo == null) {
            log.error("No ddo configuration found for the api key: {}", apiKey);
            apiResponseResolver.setStatusCode(HttpStatus.NOT_ACCEPTABLE);
            ObjectNode responseData = mapper.createObjectNode();
            responseData.put(MESSAGE, "API ".concat(apiKey).concat(EMPTY_SPACE).concat("not supported in the system"));
            apiResponseResolver.setRespData(responseData);
            return CompletableFuture.completedFuture(apiResponseResolver);
        }
        Map<String, String> queryParams = apiRequestResolver.getQueryParams();
        String additionalUriData = apiRequestResolver.getAdditionalUriData();

        String serviceUrl = redisManager.getServiceEndpoint(ddo.getService());
        String apiEndpoint = serviceUrl.concat(ddo.getApi());
        if (additionalUriData != null && !additionalUriData.isEmpty()) {
            apiEndpoint = apiEndpoint.concat(SLASH).concat(additionalUriData);
        }

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(apiEndpoint);
        if (queryParams != null && !queryParams.isEmpty()) {
            queryParams.forEach(uriBuilder::queryParam);
        }
        HttpMethod method = Utils.getHttpMethod(ddo.getMethod());
        return wsUtils.makeWSCall(uriBuilder.toUriString(), apiRequestResolver.getRequestBody(),
                apiRequestResolver.getHeaders(), method, ddo.getConnectTimeout(), ddo.getReadTimeout(), ddo.getReturnClass()).thenApplyAsync(response -> {
            log.debug("{} Received response for API key: {}", apiRequestResolver.getLoggerString(), apiRequestResolver.getApiKey());
            apiResponseResolver.setStatusCode(response.getHttpStatusCode());
            if(SUCCESS.equalsIgnoreCase(response.getStatus())){
                log.info("{} Success :: For the API key: {} received response {}", apiRequestResolver.getLoggerString(),
                        apiRequestResolver.getApiKey(), response);
                JsonNode respData = response.getData();
                apiResponseResolver.setRespData(respData);
            } else {
                log.info("{} Failure :: For the API key: {} received response {}", apiRequestResolver.getLoggerString(),
                        apiRequestResolver.getApiKey(), response);
                apiResponseResolver.setRespData(response.getData());
            }
            return apiResponseResolver;
        });
    }

    public JsonNode addCommonFieldsToRequest(ApiRequestResolver apiRequestResolver){
        if(apiRequestResolver.getRequestBody() != null && !apiRequestResolver.getRequestBody().isNull()
                && !apiRequestResolver.getRequestBody().isEmpty()){
            ObjectNode requestBody = (ObjectNode) apiRequestResolver.getRequestBody();
            ObjectNode commonFields = mapper.createObjectNode();
            commonFields.put(USER_ID, apiRequestResolver.getUserId());
            commonFields.put(JWT_TOKEN, apiRequestResolver.getJwtToken());
            commonFields.put(IP_ADDRESS, apiRequestResolver.getIpAddress());
            commonFields.put(REQ_URI, apiRequestResolver.getRequestUri());
            commonFields.put(ADDITIONAL_URI_DATA, apiRequestResolver.getAdditionalUriData());
            requestBody.set(COMMON, commonFields);
            return requestBody;
        }else{
            return apiRequestResolver.getRequestBody();
        }
    }
}

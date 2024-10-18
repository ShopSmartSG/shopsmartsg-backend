package sg.edu.nus.iss.shopsmart_backend.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import sg.edu.nus.iss.shopsmart_backend.model.DataDynamicObject;
import sg.edu.nus.iss.shopsmart_backend.model.Response;
import sg.edu.nus.iss.shopsmart_backend.redis.RedisManager;
import sg.edu.nus.iss.shopsmart_backend.service.ApiService;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class WSUtils extends ApplicationConstants {
    private static final Logger log = LoggerFactory.getLogger(WSUtils.class);
    private final ObjectMapper mapper = Json.mapper();

    private final RedisManager redisManager;

    private final RestTemplateBuilder restTemplateBuilder;

    @Autowired
    public WSUtils(RedisManager redisManager, RestTemplateBuilder restTemplateBuilder) {
        this.redisManager = redisManager;
        this.restTemplateBuilder = restTemplateBuilder;
    }

    public RestTemplate restTemplateSync(long connectTimeout, long readTimeout) {
        return restTemplateBuilder.setConnectTimeout(Duration.ofMillis(connectTimeout))
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .build();
    }

    public CompletableFuture<Response> makeWSCall(String apiKey, JsonNode data, Map<String, String> headers, Map<String, String> queryParams) {
        Response resp = new Response();
        ObjectNode responseData = mapper.createObjectNode();
        log.info("Handling request for API: {}", apiKey);
        DataDynamicObject ddo = redisManager.getDdoData(apiKey);
        if (ddo == null) {
            log.error("No ddo configuration found for the api key: {}", apiKey);
            resp.setHttpStatusCode(HttpStatus.NOT_ACCEPTABLE);
            responseData.put(MESSAGE, "API ".concat(apiKey).concat(EMPTY_SPACE).concat("not supported in the system"));
            resp.setData(responseData);
            return CompletableFuture.completedFuture(resp);
        }
        return CompletableFuture.supplyAsync(() -> {
            HttpMethod method = getHttpMethod(ddo.getMethod());
            RestTemplate restTemplate = restTemplateSync(ddo.getConnectTimeout(), ddo.getReadTimeout());
            String serviceUrl = redisManager.getServiceEndpoint(ddo.getService());

            String apiEndpoint = serviceUrl.concat(ddo.getApi());

            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(apiEndpoint);
            if (queryParams != null && !queryParams.isEmpty()) {
                queryParams.forEach(uriBuilder::queryParam);
            }

            HttpHeaders httpHeaders = new HttpHeaders();
            headers.forEach(httpHeaders::set);
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<JsonNode> request;
            if (data != null && !data.isEmpty()) {
                request = new HttpEntity<>(data, httpHeaders);
            } else {
                request = new HttpEntity<>(httpHeaders);
            }
            log.info("Making:: rest {} API {} call for {}, with request data: {}", method, apiKey, apiEndpoint, data);
            ResponseEntity<?> response = restTemplate.exchange(uriBuilder.toUriString(), method, request, Object.class);

            resp.setHttpStatusCode(response.getStatusCode());
            if(response.getBody()!=null){
                if(response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED
                        || response.getStatusCode() == HttpStatus.ACCEPTED){
                    log.info("Success:: rest {} API {} call for {} gave status : {}", method, apiKey, apiEndpoint, response.getStatusCode());
                    resp.setStatus(SUCCESS);
                }else{
                    log.error("Failed:: rest {} API {} call for {} with status code: {} and error {}", method, apiKey,
                            apiEndpoint, response.getStatusCode(), response.getBody());
                    resp.setStatus(FAILURE);
                    resp.setErrorCode(response.getStatusCode().toString());
                }
                if (response.getBody() instanceof JsonNode) {
                    resp.setData((JsonNode) response.getBody());
                    return resp;
                } else if (response.getBody() instanceof String) {
                    try {
                        responseData.set(MESSAGE, mapper.readTree((String) response.getBody()));
                        resp.setData(responseData);
                        return resp;
                    } catch (Exception e) {
                        log.error("Failed:: to parse response body for the api {} with error: ", apiKey, e);
                        responseData.put(MESSAGE, "Failed to parse api ".concat(apiKey).concat(EMPTY_SPACE).concat(RESPONSE));
                        resp.setData(responseData);
                        return resp;
                    }
                } else {
                    log.error("Exception:: Unexpected response body type: {} for apiKey {}", response.getBody().getClass(), apiKey);
                    responseData.put(MESSAGE, "Exception occurred due to unidentified body type for api ".concat(apiKey).concat(EMPTY_SPACE).concat(RESPONSE));
                    resp.setData(responseData);
                    return resp;
                }
            } else {
                responseData.put(MESSAGE, "No body found for api ".concat(apiKey).concat(EMPTY_SPACE).concat(RESPONSE));
                resp.setData(responseData);
                return resp;
            }
        });
    }

    private HttpMethod getHttpMethod(String method){
        return switch (method) {
            case "POST" -> HttpMethod.POST;
            case "PUT" -> HttpMethod.PUT;
            case "DELETE" -> HttpMethod.DELETE;
            case "PATCH" -> HttpMethod.PATCH;
            default -> HttpMethod.GET;
        };
    }
}

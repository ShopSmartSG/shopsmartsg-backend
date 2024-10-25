package sg.edu.nus.iss.shopsmart_backend.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import sg.edu.nus.iss.shopsmart_backend.model.Response;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class WSUtils extends Constants {
    private static final Logger log = LoggerFactory.getLogger(WSUtils.class);
    private final ObjectMapper mapper = Json.mapper();

    private final RestTemplateBuilder restTemplateBuilder;

    @Autowired
    public WSUtils(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplateBuilder = restTemplateBuilder;
    }

    public RestTemplate restTemplateSync(long connectTimeout, long readTimeout) {
        return restTemplateBuilder.setConnectTimeout(Duration.ofMillis(connectTimeout))
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .build();
    }

//    public CompletableFuture<Response> makeWSCall(String apiKey, JsonNode data, Map<String, String> headers,
//                                                  Map<String, String> queryParams, String additionalUriData)

    public CompletableFuture<Response> makeWSCall(String url, JsonNode data, Map<String, String> headers, HttpMethod method,
                                                  long connectTimeout, long readTImeout) {
        Response resp = new Response();
        ObjectNode responseData = mapper.createObjectNode();
        log.info("Handling request for url: {}", url);
        RestTemplate restTemplate = restTemplateSync(connectTimeout, readTImeout);

        HttpHeaders httpHeaders = new HttpHeaders();
        headers.forEach(httpHeaders::set);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<JsonNode> request;
        if (data != null && !data.isNull() && !data.isEmpty()) {
            request = new HttpEntity<>(data, httpHeaders);
        } else {
            request = new HttpEntity<>(httpHeaders);
        }
        log.info("Making:: rest {} url call for {}, with request data: {}", method, url, data);
        return CompletableFuture.supplyAsync(() -> {
            ResponseEntity<?> response = restTemplate.exchange(url, method, request, Object.class);
            resp.setHttpStatusCode(response.getStatusCode());
            if(response.getBody()!=null){
                if(response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED
                        || response.getStatusCode() == HttpStatus.ACCEPTED){
                    log.info("Success:: rest {} url call for {} gave status : {}", method, url, response.getStatusCode());
                    resp.setStatus(SUCCESS);
                }else{
                    log.error("Failed:: rest {} url call for {} with status code: {} and error {}", method, url,
                            response.getStatusCode(), response.getBody());
                    resp.setStatus(FAILURE);
                    resp.setErrorCode(response.getStatusCode().toString());
                }
                if (response.getBody() instanceof JsonNode || response.getBody() instanceof ArrayNode
                        || response.getBody() instanceof ObjectNode) {
                    resp.setData((JsonNode) response.getBody());
                    return resp;
                } else if (response.getBody() instanceof ArrayList) {
                    resp.setData(mapper.convertValue(response.getBody(), JsonNode.class));
                    return resp;
                } else if (response.getBody() instanceof String) {
                    try {
                        responseData.set(MESSAGE, mapper.readTree((String) response.getBody()));
                        resp.setData(responseData);
                        return resp;
                    } catch (Exception e) {
                        log.error("Failed:: to parse response body for the url {} with error: ", url, e);
                        responseData.put(MESSAGE, "Failed to resolve url ".concat(url).concat(EMPTY_SPACE).concat("with response: ").concat(RESPONSE));
                        resp.setData(responseData);
                        return resp;
                    }
                } else {
                    log.error("Exception:: Unexpected response body type: {} for url {}", response.getBody().getClass(), url);
                    responseData.put(MESSAGE, "Exception occurred due to unidentified body type for url ".concat(url)
                            .concat(EMPTY_SPACE).concat("with response: ").concat(RESPONSE));
                    resp.setData(responseData);
                    return resp;
                }
            } else {
                responseData.put(MESSAGE, "No response body found for url ".concat(url).concat(EMPTY_SPACE).concat(RESPONSE));
                resp.setData(responseData);
                return resp;
            }
        });
    }
}

package sg.edu.nus.iss.shopsmart_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.model.ApiResponseResolver;
import sg.edu.nus.iss.shopsmart_backend.model.DataDynamicObject;
import sg.edu.nus.iss.shopsmart_backend.model.Response;
import sg.edu.nus.iss.shopsmart_backend.utils.Constants;
import sg.edu.nus.iss.shopsmart_backend.utils.RedisManager;
import sg.edu.nus.iss.shopsmart_backend.utils.WSUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class ApiServiceTest extends Constants {
    private final ObjectMapper objectMapper = Json.mapper();

    @Mock
    private WSUtils wsUtils;

    @Mock
    private RedisManager redisManager;

    @InjectMocks
    private ApiService apiService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testProcessApiRequest_DdoNotFound() throws Exception {
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setApiKey("ddo-key");
        when(redisManager.getDdoData(anyString())).thenReturn(null);
        ApiResponseResolver response = apiService.processApiRequest(apiRequestResolver).get();
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
        assertEquals("API ddo-key not supported in the system", response.getRespData().get("message").asText());
    }

    @Test
    public void testProcessApiRequest_Success() throws Exception {
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setApiKey("ddo-key");
        apiRequestResolver.setHeaders(new HashMap<>());
        apiRequestResolver.setRequestBody(objectMapper.createObjectNode());
        apiRequestResolver.setQueryParams(Map.of("key", "value"));
        apiRequestResolver.setAdditionalUriData("additional");

        DataDynamicObject ddo = new DataDynamicObject();
        ddo.setService("service");
        ddo.setApi("api");
        ddo.setMethod(String.valueOf(HttpMethod.GET));

        Response apiResponse = new Response();
        apiResponse.setHttpStatusCode(HttpStatus.OK);
        apiResponse.setData(objectMapper.createObjectNode());
        apiResponse.setStatus(SUCCESS);

        when(redisManager.getDdoData(anyString())).thenReturn(ddo);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://service:80/");
        when(wsUtils.makeWSCall(anyString(), any(), any(), any(), anyLong(), anyLong(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(apiResponse));

        ApiResponseResolver response = apiService.processApiRequest(apiRequestResolver).get();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(objectMapper.createObjectNode(), response.getRespData());
    }

    @Test
    public void testProcessApiRequest_Failure() throws Exception {
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setApiKey("ddo-key");
        apiRequestResolver.setHeaders(new HashMap<>());
        apiRequestResolver.setRequestBody(objectMapper.createObjectNode());
        apiRequestResolver.setQueryParams(Map.of("key", "value"));
        apiRequestResolver.setAdditionalUriData("additional");

        DataDynamicObject ddo = new DataDynamicObject();
        ddo.setService("service");
        ddo.setApi("api");
        ddo.setMethod(String.valueOf(HttpMethod.GET));

        Response apiResponse = new Response();
        apiResponse.setHttpStatusCode(HttpStatus.NOT_FOUND);
        apiResponse.setData(objectMapper.createObjectNode());
        apiResponse.setStatus(FAILURE);

        when(redisManager.getDdoData(anyString())).thenReturn(ddo);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://service:80/");
        when(wsUtils.makeWSCall(anyString(), any(), any(), any(), anyLong(), anyLong(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(apiResponse));

        ApiResponseResolver response = apiService.processApiRequest(apiRequestResolver).get();
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(objectMapper.createObjectNode(), response.getRespData());
    }

    @Test
    public void testAddCommonFieldsToRequest_Added() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("key", "value");

        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(payload);

        apiService.addCommonFieldsToRequest(apiRequestResolver);
        assertTrue(apiRequestResolver.getRequestBody().has("key"));
        assertTrue(apiRequestResolver.getRequestBody().has("common"));
        assertTrue(apiRequestResolver.getRequestBody().get("common").has(IP_ADDRESS));
    }

    @Test
    public void testAddCommonFieldsToRequest_NotAdded(){
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(objectMapper.createObjectNode());

        apiService.addCommonFieldsToRequest(apiRequestResolver);
        assertFalse(apiRequestResolver.getRequestBody().has("key"));
        assertFalse(apiRequestResolver.getRequestBody().has("common"));
    }
}
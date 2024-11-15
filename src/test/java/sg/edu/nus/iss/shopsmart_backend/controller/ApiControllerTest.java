package sg.edu.nus.iss.shopsmart_backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.model.ApiResponseResolver;
import sg.edu.nus.iss.shopsmart_backend.service.ApiService;
import sg.edu.nus.iss.shopsmart_backend.service.CommonService;
import sg.edu.nus.iss.shopsmart_backend.utils.Utils;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

class ApiControllerTest {
    private final ObjectMapper objectMapper = Json.mapper();
    @Mock
    private ObjectMapper mapper;

    @Mock
    private ApiService apiService;
    @Mock
    private CommonService commonService;
    @Mock
    private Utils utils;
    @Mock
    private HttpServletResponse httpServletResponse;
    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private ApiController apiController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testHandleGetRequest() throws Exception {
        commonMocking();

        ResponseEntity<JsonNode> response = apiController.handleGetRequest("test",
                httpServletRequest, httpServletResponse).get();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testHandlePostRequest() throws Exception {
        commonMocking();

        ResponseEntity<JsonNode> response = apiController.handlePostRequest("test", objectMapper.createObjectNode(),
                httpServletRequest, httpServletResponse).get();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testHandlePutRequest() throws Exception {
        commonMocking();

        ResponseEntity<JsonNode> response = apiController.handlePutRequest("test", objectMapper.createObjectNode(),
                httpServletRequest, httpServletResponse).get();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testHandlePatchRequest() throws Exception {
        commonMocking();

        ResponseEntity<JsonNode> response = apiController.handlePatchRequest("test", objectMapper.createObjectNode(),
                httpServletRequest, httpServletResponse).get();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testHandleDeleteRequest() throws Exception {
        commonMocking();

        ResponseEntity<JsonNode> response = apiController.handleDeleteRequest("test",
                httpServletRequest, httpServletResponse).get();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private void commonMocking(){
        ApiResponseResolver apiResponseResolver = new ApiResponseResolver();
        apiResponseResolver.setStatusCode(HttpStatus.OK);
        apiResponseResolver.setRespData(objectMapper.createObjectNode());
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        when(commonService.createApiResolverRequest(any(), anyString(), any())).thenReturn(apiRequestResolver);
        when(apiService.processApiRequest(apiRequestResolver)).thenReturn(CompletableFuture.completedFuture(apiResponseResolver));
        doNothing().when(utils).setUserIdCookieNeededOrRemove(any(), any(), any());
        doNothing().when(utils).setSessionAndCookieDataForSession(any(), any(), any());
    }
}
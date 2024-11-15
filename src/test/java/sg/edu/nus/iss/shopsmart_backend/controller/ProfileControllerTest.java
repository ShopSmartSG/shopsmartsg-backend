package sg.edu.nus.iss.shopsmart_backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import sg.edu.nus.iss.shopsmart_backend.service.CommonService;
import sg.edu.nus.iss.shopsmart_backend.service.ProfileService;
import sg.edu.nus.iss.shopsmart_backend.utils.Constants;
import sg.edu.nus.iss.shopsmart_backend.utils.Utils;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

class ProfileControllerTest extends Constants {
    private final ObjectMapper objectMapper = Json.mapper();

    @Mock
    private ProfileService profileService;
    @Mock
    private CommonService commonService;
    @Mock
    private Utils utils;
    @Mock
    private HttpServletResponse httpServletResponse;
    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private ProfileController profileController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGenerateOtpForRegister() throws Exception {
        ApiResponseResolver apiResponseResolver = new ApiResponseResolver();
        apiResponseResolver.setStatusCode(HttpStatus.OK);
        apiResponseResolver.setRespData(objectMapper.createObjectNode());

        commonMocking();

        when(profileService.generateOtpForRegister(any(), anyString())).thenReturn(CompletableFuture.completedFuture(apiResponseResolver));
        ResponseEntity<JsonNode> response = profileController.generateOtpForRegister(CUSTOMER, objectMapper.createObjectNode(),
                httpServletRequest, httpServletResponse).get();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(objectMapper.createObjectNode(), response.getBody());
    }

    @Test
    public void testVerifyOtpForRegister() throws Exception {
        ObjectNode respData = objectMapper.createObjectNode();
        respData.put(USER_ID, "1234");
        ApiResponseResolver apiResponseResolver = new ApiResponseResolver();
        apiResponseResolver.setStatusCode(HttpStatus.OK);
        apiResponseResolver.setRespData(respData);

        commonMocking();
        doNothing().when(commonService).updateUserIdInRedisInSessionData(any());

        when(profileService.validateOtpAndRegister(any(), anyString())).thenReturn(CompletableFuture.completedFuture(apiResponseResolver));
        ResponseEntity<JsonNode> response = profileController.verifyOtpForRegister(CUSTOMER, objectMapper.createObjectNode(),
                httpServletRequest, httpServletResponse).get();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(respData, response.getBody());
    }

    @Test
    public void testGenerateOtpForLogin() throws Exception {
        ApiResponseResolver apiResponseResolver = new ApiResponseResolver();
        apiResponseResolver.setStatusCode(HttpStatus.OK);
        apiResponseResolver.setRespData(objectMapper.createObjectNode());

        commonMocking();

        when(profileService.generateOtpForLogin(any(), anyString())).thenReturn(CompletableFuture.completedFuture(apiResponseResolver));
        ResponseEntity<JsonNode> response = profileController.generateOtpForLogin(CUSTOMER, objectMapper.createObjectNode(),
                httpServletRequest, httpServletResponse).get();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(objectMapper.createObjectNode(), response.getBody());
    }

    @Test
    public void testVerifyOtpForLogin() throws Exception {
        ObjectNode respData = objectMapper.createObjectNode();
        respData.put(USER_ID, "1234");
        ApiResponseResolver apiResponseResolver = new ApiResponseResolver();
        apiResponseResolver.setStatusCode(HttpStatus.OK);
        apiResponseResolver.setRespData(respData);

        commonMocking();
        doNothing().when(commonService).updateUserIdInRedisInSessionData(any());

        when(profileService.validateOtpAndLogin(any(), anyString())).thenReturn(CompletableFuture.completedFuture(apiResponseResolver));
        ResponseEntity<JsonNode> response = profileController.verifyOtpForLogin(CUSTOMER, objectMapper.createObjectNode(),
                httpServletRequest, httpServletResponse).get();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(respData, response.getBody());
    }

    private void commonMocking(){
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        when(commonService.createApiResolverRequest(any(), anyString(), any())).thenReturn(apiRequestResolver);
        doNothing().when(utils).setUserIdCookieNeededOrRemove(any(), any(), any());
        doNothing().when(utils).setSessionAndCookieDataForSession(any(), any(), any());
    }
}
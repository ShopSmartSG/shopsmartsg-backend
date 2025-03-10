package sg.edu.nus.iss.shopsmart_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.model.ApiResponseResolver;
import sg.edu.nus.iss.shopsmart_backend.model.DataDynamicObject;
import sg.edu.nus.iss.shopsmart_backend.model.Response;
import sg.edu.nus.iss.shopsmart_backend.utils.ApplicationConstants;
import sg.edu.nus.iss.shopsmart_backend.utils.Constants;
import sg.edu.nus.iss.shopsmart_backend.utils.RedisManager;
import sg.edu.nus.iss.shopsmart_backend.utils.WSUtils;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class ProfileServiceTest extends Constants {
    private final ObjectMapper objectMapper = Json.mapper();

    @Mock
    private RedisManager redisManager;
    @Mock
    private WSUtils wsUtils;

    @InjectMocks
    private ProfileService profileService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGenerateOtp_Success() throws Exception{
        DataDynamicObject ddo = getDdo("service", "GET", "api", 1000, 30000);
        Response resp = new Response();
        resp.setStatus(SUCCESS);

        when(redisManager.getDdoData(anyString())).thenReturn(ddo);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://localhost:8080");
        when(wsUtils.makeWSCall(anyString(), any(), any(), any(), anyLong(), anyLong(), anyString())).thenReturn(CompletableFuture.completedFuture(resp));

        Boolean otpResp = profileService.generateOtp(new ApiRequestResolver(), "abc@mail.com", "customer").get();
        assertTrue(otpResp);
    }

    @Test
    public void testGenerateOtp_Failure() throws Exception{
        DataDynamicObject ddo = getDdo("service", "GET", "api", 1000, 30000);
        Response resp = new Response();
        resp.setStatus(FAILURE);

        when(redisManager.getDdoData(anyString())).thenReturn(ddo);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://localhost:8080");
        when(wsUtils.makeWSCall(anyString(), any(), any(), any(), anyLong(), anyLong(), anyString())).thenReturn(CompletableFuture.completedFuture(resp));

        Boolean otpResp = profileService.generateOtp(new ApiRequestResolver(), "abc@mail.com", "customer").get();
        assertFalse(otpResp);
    }

    @Test
    public void testValidateOtp_Success() throws Exception{
        DataDynamicObject ddo = getDdo("service", "GET", "api", 1000, 30000);
        Response resp = new Response();
        resp.setStatus(SUCCESS);

        when(redisManager.getDdoData(anyString())).thenReturn(ddo);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://localhost:8080");
        when(wsUtils.makeWSCall(anyString(), any(), any(), any(), anyLong(), anyLong(), anyString())).thenReturn(CompletableFuture.completedFuture(resp));

        Boolean otpResp = profileService.validateOtp(new ApiRequestResolver(), "abc@mail.com", "123456", "customer").get();
        assertTrue(otpResp);
    }

    @Test
    public void testValidateOtp_Failure() throws Exception{
        DataDynamicObject ddo = getDdo("service", "GET", "api", 1000, 30000);
        Response resp = new Response();
        resp.setStatus(FAILURE);

        when(redisManager.getDdoData(anyString())).thenReturn(ddo);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://localhost:8080");
        when(wsUtils.makeWSCall(anyString(), any(), any(), any(), anyLong(), anyLong(), anyString())).thenReturn(CompletableFuture.completedFuture(resp));

        Boolean otpResp = profileService.validateOtp(new ApiRequestResolver(), "abc@mail.com", "123456", "customer").get();
        assertFalse(otpResp);
    }

    @Test
    public void testCreateProfile_Admin() throws Exception{
        DataDynamicObject ddo = getDdo("service", "GET", "api", 1000, 30000);
        Response resp = new Response();
        resp.setStatus(SUCCESS);

        when(redisManager.getDdoData(anyString())).thenReturn(ddo);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://localhost:8080");
        when(wsUtils.makeWSCall(anyString(), any(), any(), any(), anyLong(), anyLong(), anyString())).thenReturn(CompletableFuture.completedFuture(resp));

        Boolean otpResp = profileService.createProfile(new ApiRequestResolver(), ApplicationConstants.ADMIN).get();
        assertTrue(otpResp);
    }

    @Test
    public void testCreateProfile_Success() throws Exception{
        DataDynamicObject ddo = getDdo("service", "GET", "api", 1000, 30000);
        Response resp = new Response();
        resp.setStatus(SUCCESS);

        when(redisManager.getDdoData(anyString())).thenReturn(ddo);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://localhost:8080");
        when(wsUtils.makeWSCall(anyString(), any(), any(), any(), anyLong(), anyLong(), anyString())).thenReturn(CompletableFuture.completedFuture(resp));

        Boolean otpResp = profileService.createProfile(new ApiRequestResolver(), ApplicationConstants.CUSTOMER).get();
        assertTrue(otpResp);
    }

    @Test
    public void testCreateProfile_Failure() throws Exception{
        DataDynamicObject ddo = getDdo("service", "GET", "api", 1000, 30000);
        Response resp = new Response();
        resp.setStatus(FAILURE);

        when(redisManager.getDdoData(anyString())).thenReturn(ddo);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://localhost:8080");
        when(wsUtils.makeWSCall(anyString(), any(), any(), any(), anyLong(), anyLong(), anyString())).thenReturn(CompletableFuture.completedFuture(resp));

        Boolean otpResp = profileService.createProfile(new ApiRequestResolver(), ApplicationConstants.CUSTOMER).get();
        assertFalse(otpResp);
    }

    @Test
    public void testCreateProfile_Invalid_Prof_Type() throws Exception{
        Boolean otpResp = profileService.createProfile(new ApiRequestResolver(), "invalid").get();
        assertFalse(otpResp);
    }

    @Test
    public void testFetchUserIdForEmail_Admin_Success() throws Exception{
        when(redisManager.getHashValue(anyString(), eq(ADMIN_EMAIL_ID))).thenReturn("abc@mail.com");
        when(redisManager.getHashValue(anyString(), eq(ADMIN_USER_ID))).thenReturn("123456");

        String otpResp = profileService.fetchUserIdForEmail(new ApiRequestResolver(), "abc@mail.com", ADMIN).get();
        assertEquals("123456", otpResp);
    }

    @Test
    public void testFetchUserIdForEmail_Admin_Failure() throws Exception{
        when(redisManager.getHashValue(anyString(), anyString())).thenReturn("def@mail.com");

        String otpResp = profileService.fetchUserIdForEmail(new ApiRequestResolver(), "abc@mail.com", ADMIN).get();
        assertEquals("", otpResp);
    }

    @Test
    public void testFetchUserIdForEmail_Admin_Failure2() throws Exception{
        when(redisManager.getHashValue(anyString(), anyString())).thenReturn(null);

        String otpResp = profileService.fetchUserIdForEmail(new ApiRequestResolver(), "abc@mail.com", ADMIN).get();
        assertEquals("", otpResp);
    }

    @Test
    public void testFetchUserIdForEmail_Success() throws Exception{
        DataDynamicObject ddo = getDdo("service", "GET", "api", 1000, 30000);
        ObjectNode data = objectMapper.createObjectNode();
        data.put(MESSAGE, "\"804jt408\"");
        Response resp = new Response();
        resp.setStatus(SUCCESS);
        resp.setData(data);

        when(redisManager.getDdoData(anyString())).thenReturn(ddo);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://localhost:8080");
        when(wsUtils.makeWSCall(anyString(), any(), any(), any(), anyLong(), anyLong(), anyString())).thenReturn(CompletableFuture.completedFuture(resp));

        String otpResp = profileService.fetchUserIdForEmail(new ApiRequestResolver(), "abc@mail.com", CUSTOMER).get();
        assertEquals("804jt408", otpResp);
    }

    @Test
    public void testFetchUserIdForEmail_Failure() throws Exception{
        DataDynamicObject ddo = getDdo("service", "GET", "api", 1000, 30000);
        Response resp = new Response();
        resp.setStatus(FAILURE);

        when(redisManager.getDdoData(anyString())).thenReturn(ddo);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://localhost:8080");
        when(wsUtils.makeWSCall(anyString(), any(), any(), any(), anyLong(), anyLong(), anyString())).thenReturn(CompletableFuture.completedFuture(resp));

        String otpResp = profileService.fetchUserIdForEmail(new ApiRequestResolver(), "abc@mail.com", CUSTOMER).get();
        assertEquals("", otpResp);
    }

    @Test
    public void testFetchUserIdForEmail_Invalid_Prof_Type() throws Exception{
        String otpResp = profileService.fetchUserIdForEmail(new ApiRequestResolver(), "abc@mail.com", "invalid").get();
        assertEquals("", otpResp);
    }

    private DataDynamicObject getDdo(String service, String method, String api, int connectTimeout, int readTimeout) {
        DataDynamicObject ddo = new DataDynamicObject();
        ddo.setService(service);
        ddo.setMethod(method);
        ddo.setApi(api);
        ddo.setConnectTimeout(connectTimeout);
        ddo.setReadTimeout(readTimeout);
        return ddo;
    }
}
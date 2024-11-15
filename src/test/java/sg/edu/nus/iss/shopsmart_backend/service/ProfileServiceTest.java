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
    public void testGenerateOtpForRegister_NoPayload() throws Exception {
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(objectMapper.nullNode());

        ApiResponseResolver responseResolver = profileService.generateOtpForRegister(apiRequestResolver, CUSTOMER).get();
        assertEquals(HttpStatus.BAD_REQUEST, responseResolver.getStatusCode());
        assertEquals("No request body found for otp generation for email", responseResolver.getRespData().get("message").asText());
    }

    @Test
    public void testGenerateOtpForRegister_NoEmail() throws Exception {
        ObjectNode reqBody = objectMapper.createObjectNode();
        reqBody.put("otp", "123456");
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(reqBody);

        ApiResponseResolver responseResolver = profileService.generateOtpForRegister(apiRequestResolver, CUSTOMER).get();
        assertEquals(HttpStatus.BAD_REQUEST, responseResolver.getStatusCode());
        assertEquals("Email is required for generating OTP for registration", responseResolver.getRespData().get("message").asText());
    }

    @Test
    public void testGenerateOtpForRegister_UnAuthAdmin() throws Exception {
        ObjectNode reqBody = objectMapper.createObjectNode();
        reqBody.put(EMAIL, "admin1223@mail.com");
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(reqBody);

        when(redisManager.getHashValue(anyString(), anyString())).thenReturn("admin@mail.com");

        ApiResponseResolver responseResolver = profileService.generateOtpForRegister(apiRequestResolver, ADMIN).get();
        assertEquals(HttpStatus.UNAUTHORIZED, responseResolver.getStatusCode());
        assertEquals("Email is not authorized for admin OTP generation", responseResolver.getRespData().get("message").asText());
    }

    @Test
    public void testGenerateOtpForRegister_AdminUser() throws Exception {
        ObjectNode reqBody = objectMapper.createObjectNode();
        reqBody.put(EMAIL, "admin@mail.com");
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(reqBody);

        when(redisManager.getHashValue(anyString(), eq(ADMIN_EMAIL_ID))).thenReturn("admin@mail.com");
        when(redisManager.getHashValue(anyString(), eq(ADMIN_USER_ID))).thenReturn("admin123");

        ApiResponseResolver responseResolver = profileService.generateOtpForRegister(apiRequestResolver, ADMIN).get();
        assertEquals(HttpStatus.CONFLICT, responseResolver.getStatusCode());
        assertEquals("User already exists with email, please login", responseResolver.getRespData().get("message").asText());
    }

    @Test
    public void testGenerateOtpForRegister_ExistingProfileUser() throws Exception {
        ObjectNode reqBody = objectMapper.createObjectNode();
        reqBody.put(EMAIL, "user@mail.com");
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(reqBody);

        DataDynamicObject ddo = getDdo("service", "GET", "api", 1000, 30000);

        ObjectNode data = objectMapper.createObjectNode();
        data.put(MESSAGE, "\"804jt408\"");
        Response resp = new Response();
        resp.setStatus(SUCCESS);
        resp.setData(data);

        when(redisManager.getDdoData(anyString())).thenReturn(ddo);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://localhost:8080");
        when(wsUtils.makeWSCall(anyString(), any(), any(), any(), anyLong(), anyLong(), anyString())).thenReturn(CompletableFuture.completedFuture(resp));

        ApiResponseResolver responseResolver = profileService.generateOtpForRegister(apiRequestResolver, CUSTOMER).get();
        assertEquals(HttpStatus.CONFLICT, responseResolver.getStatusCode());
        assertEquals("User already exists with email, please login", responseResolver.getRespData().get("message").asText());
    }

    //dont need to write success as it will get covered in success for generateOtpForLogin
    @Test
    public void testGenerateOtpForRegister_Failure() throws Exception {
        ObjectNode reqBody = objectMapper.createObjectNode();
        reqBody.put(EMAIL, "user@mail.com");
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(reqBody);

        DataDynamicObject fetchCustId = getDdo("service", "GET", "api", 1000, 30000);
        DataDynamicObject genOtp = getDdo("service", "POST", "gen-otp", 1000, 30000);

        Response resp = new Response();
        resp.setStatus(FAILURE);
        resp.setHttpStatusCode(HttpStatus.NOT_FOUND);

        when(redisManager.getDdoData(FETCH_CUSTOMER_ID_BY_EMAIL)).thenReturn(fetchCustId);
        when(redisManager.getDdoData(GENERATE_OTP)).thenReturn(genOtp);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://localhost:8080");
        when(wsUtils.makeWSCall(anyString(), any(), any(), any(), anyLong(), anyLong(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(resp));

        ApiResponseResolver responseResolver = profileService.generateOtpForRegister(apiRequestResolver, CUSTOMER).get();
        assertEquals(HttpStatus.NOT_FOUND, responseResolver.getStatusCode());
        assertEquals("Error generating OTP", responseResolver.getRespData().get("message").asText());
    }

    @Test
    public void testGenerateOtpForLogin_NoPayload() throws Exception {
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(objectMapper.nullNode());

        ApiResponseResolver responseResolver = profileService.generateOtpForLogin(apiRequestResolver, CUSTOMER).get();
        assertEquals(HttpStatus.BAD_REQUEST, responseResolver.getStatusCode());
        assertEquals("No request body found for otp generation for email", responseResolver.getRespData().get("message").asText());
    }

    @Test
    public void testGenerateOtpForLogin_NoEmail() throws Exception {
        ObjectNode reqBody = objectMapper.createObjectNode();
        reqBody.put("otp", "123456");
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(reqBody);

        ApiResponseResolver responseResolver = profileService.generateOtpForLogin(apiRequestResolver, CUSTOMER).get();
        assertEquals(HttpStatus.BAD_REQUEST, responseResolver.getStatusCode());
        assertEquals("Email is required for generating OTP for login", responseResolver.getRespData().get("message").asText());
    }

    @Test
    public void testGenerateOtpForLogin_NoExistingProfile() throws Exception {
        ObjectNode reqBody = objectMapper.createObjectNode();
        reqBody.put(EMAIL, "user@mail.com");
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(reqBody);

        DataDynamicObject ddo = getDdo("service", "GET", "api", 1000, 30000);

        Response resp = new Response();
        resp.setStatus(FAILURE);

        when(redisManager.getDdoData(anyString())).thenReturn(ddo);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://localhost:8080");
        when(wsUtils.makeWSCall(anyString(), any(), any(), any(), anyLong(), anyLong(), anyString())).thenReturn(CompletableFuture.completedFuture(resp));

        ApiResponseResolver responseResolver = profileService.generateOtpForLogin(apiRequestResolver, CUSTOMER).get();
        assertEquals(HttpStatus.NOT_FOUND, responseResolver.getStatusCode());
        assertEquals("User does not exists with email, please register", responseResolver.getRespData().get("message").asText());
    }

    //dont need to write failure case as it will get covered in failure for generateOtpForRegister
    @Test
    public void testGenerateOtpForLogin_Success() throws Exception {
        ObjectNode reqBody = objectMapper.createObjectNode();
        reqBody.put(EMAIL, "user@mail.com");
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(reqBody);

        DataDynamicObject fetchCustId = getDdo("service", "GET", "api", 1000, 30000);
        DataDynamicObject genOtp = getDdo("service", "POST", "gen-otp", 1000, 30000);

        ObjectNode data = objectMapper.createObjectNode();
        data.put(MESSAGE, "\"804jt408\"");
        Response resp = new Response();
        resp.setStatus(SUCCESS);
        resp.setHttpStatusCode(HttpStatus.OK);
        resp.setData(data);

        when(redisManager.getDdoData(FETCH_CUSTOMER_ID_BY_EMAIL)).thenReturn(fetchCustId);
        when(redisManager.getDdoData(GENERATE_OTP)).thenReturn(genOtp);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://localhost:8080");
        when(wsUtils.makeWSCall(anyString(), any(), any(), any(), anyLong(), anyLong(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(resp));

        ApiResponseResolver responseResolver = profileService.generateOtpForLogin(apiRequestResolver, CUSTOMER).get();
        assertEquals(HttpStatus.OK, responseResolver.getStatusCode());
        assertEquals("OTP generated successfully", responseResolver.getRespData().get("message").asText());
    }

    @Test
    public void testValidateOtpAndRegister_NoPayload() throws Exception {
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(objectMapper.nullNode());

        ApiResponseResolver responseResolver = profileService.validateOtpAndRegister(apiRequestResolver, CUSTOMER).get();
        assertEquals(HttpStatus.BAD_REQUEST, responseResolver.getStatusCode());
        assertEquals("No request body found for otp validation of email", responseResolver.getRespData().get("message").asText());
    }

    @Test
    public void testValidateOtpAndRegister_NoEmailOrOtp() throws Exception {
        ObjectNode reqBody = objectMapper.createObjectNode();
        reqBody.put("pass", "123456");
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(reqBody);

        ApiResponseResolver responseResolver = profileService.validateOtpAndRegister(apiRequestResolver, CUSTOMER).get();
        assertEquals(HttpStatus.BAD_REQUEST, responseResolver.getStatusCode());
        assertEquals("Email and OTP are required for validating OTP for registration", responseResolver.getRespData().get("message").asText());
    }

    @Test
    public void testValidateOtpAndRegister_OtpVerifyFailed() throws Exception {
        ObjectNode reqBody = objectMapper.createObjectNode();
        reqBody.put(EMAIL, "user@mail.com");
        reqBody.put(OTP, "123456");
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(reqBody);

        DataDynamicObject valOtp = getDdo("service", "POST", "val-otp", 1000, 30000);

        Response resp = new Response();
        resp.setStatus(FAILURE);
        when(redisManager.getDdoData(anyString())).thenReturn(valOtp);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://localhost:8080/");
        when(wsUtils.makeWSCall(anyString(), any(), any(), any(), anyLong(), anyLong(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(resp));

        ApiResponseResolver responseResolver = profileService.validateOtpAndRegister(apiRequestResolver, CUSTOMER).get();
        assertEquals(HttpStatus.UNAUTHORIZED, responseResolver.getStatusCode());
        assertEquals("OTP validation failed, please try again", responseResolver.getRespData().get("message").asText());
    }

    @Test
    public void testValidateOtpAndRegister_AdminProfCreate() throws Exception {
        ObjectNode reqBody = objectMapper.createObjectNode();
        reqBody.put(EMAIL, "user@mail.com");
        reqBody.put(OTP, "123456");
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(reqBody);

        DataDynamicObject valOtp = getDdo("service", "POST", "val-otp", 1000, 30000);

        Response resp = new Response();
        resp.setStatus(SUCCESS);
        when(redisManager.getDdoData(anyString())).thenReturn(valOtp);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://localhost:8080");
        when(redisManager.getHashValue(anyString(), anyString())).thenReturn("admin1234");
        when(wsUtils.makeWSCall(anyString(), any(), any(), any(), anyLong(), anyLong(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(resp));

        ApiResponseResolver responseResolver = profileService.validateOtpAndRegister(apiRequestResolver, ADMIN).get();
        assertEquals(HttpStatus.OK, responseResolver.getStatusCode());
        assertEquals("admin1234", responseResolver.getRespData().get(USER_ID).asText());

    }

    @Test
    public void testValidateOtpAndRegister_CreateProfileFailed() throws Exception {
        ObjectNode reqBody = objectMapper.createObjectNode();
        reqBody.put(EMAIL, "user@mail.com");
        reqBody.put(OTP, "123456");
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(reqBody);

        DataDynamicObject valOtp = getDdo("service", "POST", "val-otp", 1000, 30000);
        Response valOtpResp = new Response();
        valOtpResp.setStatus(SUCCESS);

        DataDynamicObject createProf = getDdo("service", "POST", "create-prof", 1000, 30000);
        Response createProfResp = new Response();
        createProfResp.setStatus(FAILURE);

        when(redisManager.getDdoData(VALIDATE_OTP)).thenReturn(valOtp);
        when(redisManager.getDdoData(CREATE_CUSTOMER_PROFILE)).thenReturn(createProf);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://localhost:8080/");
        when(wsUtils.makeWSCall(eq("http://localhost:8080/val-otp?email=user@mail.com&otp=123456"), any(), any(),
                any(), anyLong(), anyLong(), anyString())).thenReturn(CompletableFuture.completedFuture(valOtpResp));
        when(wsUtils.makeWSCall(eq("http://localhost:8080/create-prof"), any(), any(), any(), anyLong(), anyLong(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(createProfResp));

        ApiResponseResolver responseResolver = profileService.validateOtpAndRegister(apiRequestResolver, CUSTOMER).get();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseResolver.getStatusCode());
        assertEquals("Some error occurred while creating profile, please try again.", responseResolver.getRespData().get(MESSAGE).asText());
    }

    @Test
    public void testValidateOtpAndRegister_CreateProfileSuccess_FetchUserFailed() throws Exception {
        ObjectNode reqBody = objectMapper.createObjectNode();
        reqBody.put(EMAIL, "user@mail.com");
        reqBody.put(OTP, "123456");
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(reqBody);

        DataDynamicObject valOtp = getDdo("service", "POST", "val-otp", 1000, 30000);
        Response valOtpResp = new Response();
        valOtpResp.setStatus(SUCCESS);

        DataDynamicObject createProf = getDdo("service", "POST", "create-prof", 1000, 30000);
        Response createProfResp = new Response();
        createProfResp.setStatus(SUCCESS);

        DataDynamicObject fetchProfId = getDdo("service", "GET", "prof-id", 1000, 30000);
        Response fetchProfIdResp = new Response();
        fetchProfIdResp.setStatus(FAILURE);

        when(redisManager.getDdoData(VALIDATE_OTP)).thenReturn(valOtp);
        when(redisManager.getDdoData(CREATE_CUSTOMER_PROFILE)).thenReturn(createProf);
        when(redisManager.getDdoData(FETCH_CUSTOMER_ID_BY_EMAIL)).thenReturn(fetchProfId);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://localhost:8080/");
        when(wsUtils.makeWSCall(eq("http://localhost:8080/val-otp?email=user@mail.com&otp=123456"), any(), any(),
                any(), anyLong(), anyLong(), anyString())).thenReturn(CompletableFuture.completedFuture(valOtpResp));
        when(wsUtils.makeWSCall(eq("http://localhost:8080/create-prof"), any(), any(), any(), anyLong(), anyLong(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(createProfResp));
        when(wsUtils.makeWSCall(eq("http://localhost:8080/prof-id/user@mail.com"), any(), any(), any(), anyLong(), anyLong(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(fetchProfIdResp));

        ApiResponseResolver responseResolver = profileService.validateOtpAndRegister(apiRequestResolver, CUSTOMER).get();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseResolver.getStatusCode());
        assertEquals("Some error occurred while creating profile, please try again.", responseResolver.getRespData().get(MESSAGE).asText());
    }

    @Test
    public void testValidateOtpAndRegister_Success() throws Exception {
        ObjectNode reqBody = objectMapper.createObjectNode();
        reqBody.put(EMAIL, "user@mail.com");
        reqBody.put(OTP, "123456");
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(reqBody);

        DataDynamicObject valOtp = getDdo("service", "POST", "val-otp", 1000, 30000);
        Response valOtpResp = new Response();
        valOtpResp.setStatus(SUCCESS);

        DataDynamicObject createProf = getDdo("service", "POST", "create-prof", 1000, 30000);
        Response createProfResp = new Response();
        createProfResp.setStatus(SUCCESS);

        DataDynamicObject fetchProfId = getDdo("service", "GET", "prof-id", 1000, 30000);
        ObjectNode data = objectMapper.createObjectNode();
        data.put(MESSAGE, "\"804jt408\"");
        Response fetchProfIdResp = new Response();
        fetchProfIdResp.setStatus(SUCCESS);
        fetchProfIdResp.setData(data);

        when(redisManager.getDdoData(VALIDATE_OTP)).thenReturn(valOtp);
        when(redisManager.getDdoData(CREATE_CUSTOMER_PROFILE)).thenReturn(createProf);
        when(redisManager.getDdoData(FETCH_CUSTOMER_ID_BY_EMAIL)).thenReturn(fetchProfId);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://localhost:8080/");
        when(wsUtils.makeWSCall(eq("http://localhost:8080/val-otp?email=user@mail.com&otp=123456"), any(), any(),
                any(), anyLong(), anyLong(), anyString())).thenReturn(CompletableFuture.completedFuture(valOtpResp));
        when(wsUtils.makeWSCall(eq("http://localhost:8080/create-prof"), any(), any(), any(), anyLong(), anyLong(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(createProfResp));
        when(wsUtils.makeWSCall(eq("http://localhost:8080/prof-id/user@mail.com"), any(), any(), any(), anyLong(), anyLong(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(fetchProfIdResp));

        ApiResponseResolver responseResolver = profileService.validateOtpAndRegister(apiRequestResolver, CUSTOMER).get();
        assertEquals(HttpStatus.OK, responseResolver.getStatusCode());
        assertEquals("804jt408", responseResolver.getRespData().get(USER_ID).asText());
    }

    @Test
    public void testValidateOtpAndLogin_NoPayload() throws Exception {
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(objectMapper.nullNode());

        ApiResponseResolver responseResolver = profileService.validateOtpAndLogin(apiRequestResolver, CUSTOMER).get();
        assertEquals(HttpStatus.BAD_REQUEST, responseResolver.getStatusCode());
        assertEquals("No request body found for otp validation of email", responseResolver.getRespData().get("message").asText());
    }

    @Test
    public void testValidateOtpAndLogin_NoEmailOrOtp() throws Exception {
        ObjectNode reqBody = objectMapper.createObjectNode();
        reqBody.put("pass", "123456");
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(reqBody);

        ApiResponseResolver responseResolver = profileService.validateOtpAndLogin(apiRequestResolver, CUSTOMER).get();
        assertEquals(HttpStatus.BAD_REQUEST, responseResolver.getStatusCode());
        assertEquals("Email and OTP are required for validating OTP for login", responseResolver.getRespData().get("message").asText());
    }

    @Test
    public void testValidateOtpAndLogin_OtpVerifyFailed() throws Exception {
        ObjectNode reqBody = objectMapper.createObjectNode();
        reqBody.put(EMAIL, "user@mail.com");
        reqBody.put(OTP, "123456");
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(reqBody);

        DataDynamicObject valOtp = getDdo("service", "POST", "val-otp", 1000, 30000);

        Response resp = new Response();
        resp.setStatus(FAILURE);
        when(redisManager.getDdoData(anyString())).thenReturn(valOtp);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://localhost:8080/");
        when(wsUtils.makeWSCall(anyString(), any(), any(), any(), anyLong(), anyLong(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(resp));

        ApiResponseResolver responseResolver = profileService.validateOtpAndLogin(apiRequestResolver, CUSTOMER).get();
        assertEquals(HttpStatus.UNAUTHORIZED, responseResolver.getStatusCode());
        assertEquals("OTP validation failed, please try again", responseResolver.getRespData().get("message").asText());
    }

    @Test
    public void testValidateOtpAndLogin_FetchUserFailed() throws Exception {
        ObjectNode reqBody = objectMapper.createObjectNode();
        reqBody.put(EMAIL, "user@mail.com");
        reqBody.put(OTP, "123456");
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(reqBody);

        DataDynamicObject valOtp = getDdo("service", "POST", "val-otp", 1000, 30000);
        Response valOtpResp = new Response();
        valOtpResp.setStatus(SUCCESS);

        DataDynamicObject fetchProfId = getDdo("service", "GET", "prof-id", 1000, 30000);
        Response fetchProfIdResp = new Response();
        fetchProfIdResp.setStatus(FAILURE);

        when(redisManager.getDdoData(VALIDATE_OTP)).thenReturn(valOtp);
        when(redisManager.getDdoData(FETCH_CUSTOMER_ID_BY_EMAIL)).thenReturn(fetchProfId);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://localhost:8080/");
        when(wsUtils.makeWSCall(eq("http://localhost:8080/val-otp?email=user@mail.com&otp=123456"), any(), any(),
                any(), anyLong(), anyLong(), anyString())).thenReturn(CompletableFuture.completedFuture(valOtpResp));
        when(wsUtils.makeWSCall(eq("http://localhost:8080/prof-id/user@mail.com"), any(), any(), any(), anyLong(), anyLong(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(fetchProfIdResp));

        ApiResponseResolver responseResolver = profileService.validateOtpAndLogin(apiRequestResolver, CUSTOMER).get();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseResolver.getStatusCode());
        assertEquals("Some error occurred while fetching userId, please try again.", responseResolver.getRespData().get(MESSAGE).asText());
    }

    @Test
    public void testValidateOtpAndLogin_Success() throws Exception {
        ObjectNode reqBody = objectMapper.createObjectNode();
        reqBody.put(EMAIL, "user@mail.com");
        reqBody.put(OTP, "123456");
        ApiRequestResolver apiRequestResolver = new ApiRequestResolver();
        apiRequestResolver.setRequestBody(reqBody);

        DataDynamicObject valOtp = getDdo("service", "POST", "val-otp", 1000, 30000);
        Response valOtpResp = new Response();
        valOtpResp.setStatus(SUCCESS);

        DataDynamicObject fetchProfId = getDdo("service", "GET", "prof-id", 1000, 30000);
        ObjectNode data = objectMapper.createObjectNode();
        data.put(MESSAGE, "\"804jt408\"");
        Response fetchProfIdResp = new Response();
        fetchProfIdResp.setStatus(SUCCESS);
        fetchProfIdResp.setData(data);

        when(redisManager.getDdoData(VALIDATE_OTP)).thenReturn(valOtp);
        when(redisManager.getDdoData(FETCH_CUSTOMER_ID_BY_EMAIL)).thenReturn(fetchProfId);
        when(redisManager.getServiceEndpoint(anyString())).thenReturn("http://localhost:8080/");
        when(wsUtils.makeWSCall(eq("http://localhost:8080/val-otp?email=user@mail.com&otp=123456"), any(), any(),
                any(), anyLong(), anyLong(), anyString())).thenReturn(CompletableFuture.completedFuture(valOtpResp));
        when(wsUtils.makeWSCall(eq("http://localhost:8080/prof-id/user@mail.com"), any(), any(), any(), anyLong(), anyLong(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(fetchProfIdResp));

        ApiResponseResolver responseResolver = profileService.validateOtpAndLogin(apiRequestResolver, CUSTOMER).get();
        assertEquals(HttpStatus.OK, responseResolver.getStatusCode());
        assertEquals("804jt408", responseResolver.getRespData().get(USER_ID).asText());
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
package sg.edu.nus.iss.shopsmart_backend.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.model.ApiResponseResolver;
import sg.edu.nus.iss.shopsmart_backend.service.ProfileService;
import sg.edu.nus.iss.shopsmart_backend.utils.Constants;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ValidateOtpHandlerTest extends Constants {
    private ProfileService profileService;
    private ValidateOtpHandler handler;
    private Handler nextHandler;

    @BeforeEach
    void setUp() {
        profileService = mock(ProfileService.class);
        nextHandler = mock(Handler.class);
        handler = new ValidateOtpHandler(profileService);
        handler.setNext(nextHandler);
    }

    @Test
    void testHandle_EmailOrOtpIsEmpty() {
        ApiRequestResolver request = new ApiRequestResolver();
        request.addParamValue(EMAIL, "");
        request.addParamValue(OTP, "123456");

        CompletableFuture<ApiResponseResolver> response = handler.handle(request, "admin");

        ApiResponseResolver result = response.join();
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertEquals("Email or otp is empty or not found in request for otp validation",
                result.getRespData().get(Constants.MESSAGE).asText());
        verify(profileService, never()).validateOtp(any(), anyString(), anyString());
        verify(nextHandler, never()).handle(any(), anyString());
    }

    @Test
    void testHandle_OtpValidationFailed() {
        ApiRequestResolver request = new ApiRequestResolver();
        request.addParamValue(EMAIL, "test@example.com");
        request.addParamValue(OTP, "123456");

        when(profileService.validateOtp(any(), eq("test@example.com"), eq("123456")))
                .thenReturn(CompletableFuture.completedFuture(false)); // OTP validation failed

        CompletableFuture<ApiResponseResolver> response = handler.handle(request, "admin");

        ApiResponseResolver result = response.join();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertEquals("Failure occurred in validation of otp for provided request",
                result.getRespData().get(Constants.MESSAGE).asText());
        verify(nextHandler, never()).handle(any(), anyString());
    }

    @Test
    void testHandle_OtpValidationSuccess_WithNextHandler() {
        ApiRequestResolver request = new ApiRequestResolver();
        request.addParamValue(EMAIL, "test@example.com");
        request.addParamValue(OTP, "123456");

        when(profileService.validateOtp(any(), eq("test@example.com"), eq("123456")))
                .thenReturn(CompletableFuture.completedFuture(true)); // OTP validation success

        ApiResponseResolver nextHandlerResponse = new ApiResponseResolver();
        nextHandlerResponse.setStatusCode(HttpStatus.OK);
        when(nextHandler.handle(any(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(nextHandlerResponse));

        CompletableFuture<ApiResponseResolver> response = handler.handle(request, "admin");

        ApiResponseResolver result = response.join();
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(nextHandler, times(1)).handle(any(), eq("admin"));
    }

    @Test
    void testHandle_OtpValidationSuccess_NoNextHandler() {
        ApiRequestResolver request = new ApiRequestResolver();
        request.addParamValue(EMAIL, "test@example.com");
        request.addParamValue(OTP, "123456");

        handler.setNext(null); // No next handler in the chain

        when(profileService.validateOtp(any(), eq("test@example.com"), eq("123456")))
                .thenReturn(CompletableFuture.completedFuture(true)); // OTP validation success

        CompletableFuture<ApiResponseResolver> response = handler.handle(request, "admin");

        ApiResponseResolver result = response.join();
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Otp validation completed as expected", result.getRespData().get(Constants.MESSAGE).asText());
        verify(nextHandler, never()).handle(any(), anyString());
    }
}
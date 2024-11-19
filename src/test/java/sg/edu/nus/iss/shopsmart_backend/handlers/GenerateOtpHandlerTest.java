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

class GenerateOtpHandlerTest extends Constants {

    private ProfileService profileService;
    private GenerateOtpHandler handler;
    private Handler nextHandler;

    @BeforeEach
    void setUp() {
        profileService = mock(ProfileService.class);
        nextHandler = mock(Handler.class);
        handler = new GenerateOtpHandler(profileService);
        handler.setNext(nextHandler);
    }

    @Test
    void testHandle_EmailIsEmpty() {
        ApiRequestResolver request = mock(ApiRequestResolver.class);
        request.addParamValue(EMAIL, ""); // Simulate empty email

        CompletableFuture<ApiResponseResolver> response = handler.handle(request, "admin");

        ApiResponseResolver result = response.join();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertEquals("Email is empty or not found in request for otp generation", result.getRespData().get(MESSAGE).asText());
        verify(profileService, never()).generateOtp(any(), anyString());
        verify(nextHandler, never()).handle(any(), anyString());
    }

    @Test
    void testHandle_OtpGenerationFailed() {
        ApiRequestResolver request = new ApiRequestResolver();
        request.addParamValue(EMAIL, "test@example.com");

        when(profileService.generateOtp(any(), eq("test@example.com")))
                .thenReturn(CompletableFuture.completedFuture(false)); // Simulate OTP generation failure

        CompletableFuture<ApiResponseResolver> response = handler.handle(request, "admin");

        ApiResponseResolver result = response.join();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertEquals("Failure occurred in generation of otp for provided email", result.getRespData().get(MESSAGE).asText());
        verify(nextHandler, never()).handle(any(), anyString());
    }

    @Test
    void testHandle_OtpGenerated_WithNextHandler() {
        ApiRequestResolver request = new ApiRequestResolver();
        request.addParamValue(EMAIL, "test@example.com");

        when(profileService.generateOtp(any(), eq("test@example.com")))
                .thenReturn(CompletableFuture.completedFuture(true)); // Simulate successful OTP generation

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
    void testHandle_OtpGenerated_NoNextHandler() {
        ApiRequestResolver request = new ApiRequestResolver();
        request.addParamValue(EMAIL, "test@example.com");

        handler.setNext(null); // No next handler in the chain

        when(profileService.generateOtp(any(), eq("test@example.com")))
                .thenReturn(CompletableFuture.completedFuture(true)); // Simulate successful OTP generation

        CompletableFuture<ApiResponseResolver> response = handler.handle(request, "admin");

        ApiResponseResolver result = response.join();
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Otp generation completed as expected", result.getRespData().get(MESSAGE).asText());
        verify(nextHandler, never()).handle(any(), anyString());
    }
}
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

class FetchUserIdForEmailHandlerTest extends Constants {
    private ProfileService profileService;
    private FetchUserIdForEmailHandler handler;
    private Handler nextHandler;

    @BeforeEach
    void setUp() {
        profileService = mock(ProfileService.class);
        nextHandler = mock(Handler.class);
        handler = new FetchUserIdForEmailHandler(profileService);
        handler.setNext(nextHandler);
    }

    @Test
    void testHandle_EmailIsEmpty() {
        ApiRequestResolver request = new ApiRequestResolver();
        request.addParamValue(EMAIL, ""); // Simulate empty email

        CompletableFuture<ApiResponseResolver> response = handler.handle(request, "admin");

        ApiResponseResolver result = response.join();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertEquals("Email is empty or not found in request for fetching user id", result.getRespData().get("message").asText());
        verify(profileService, never()).fetchUserIdForEmail(any(), anyString(), anyString());
        verify(nextHandler, never()).handle(any(), anyString());
    }

    @Test
    void testHandle_UserIdNotFound() {
        ApiRequestResolver request = new ApiRequestResolver();
        request.addParamValue(EMAIL, "test@example.com");

        when(profileService.fetchUserIdForEmail(any(), eq("test@example.com"), anyString()))
                .thenReturn(CompletableFuture.completedFuture("")); // Simulate no userId found

        CompletableFuture<ApiResponseResolver> response = handler.handle(request, "admin");

        ApiResponseResolver result = response.join();
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertEquals("No existing profile found for the email", result.getRespData().get("message").asText());
        verify(nextHandler, never()).handle(any(), anyString());
    }

    @Test
    void testHandle_UserIdFound_WithNextHandler() {
        ApiRequestResolver request = new ApiRequestResolver();
        request.addParamValue(EMAIL, "test@example.com");

        when(profileService.fetchUserIdForEmail(any(), eq("test@example.com"), anyString()))
                .thenReturn(CompletableFuture.completedFuture("user123")); // Simulate userId found

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
    void testHandle_UserIdFound_NoNextHandler() {
        ApiRequestResolver request = new ApiRequestResolver();
        request.addParamValue(EMAIL, "test@example.com");

        handler.setNext(null); // No next handler

        when(profileService.fetchUserIdForEmail(any(), eq("test@example.com"), anyString()))
                .thenReturn(CompletableFuture.completedFuture("user123")); // Simulate userId found

        CompletableFuture<ApiResponseResolver> response = handler.handle(request, "admin");

        ApiResponseResolver result = response.join();
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("user123", result.getRespData().get(Constants.USER_ID).asText());
    }
}
package sg.edu.nus.iss.shopsmart_backend.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.model.ApiResponseResolver;
import sg.edu.nus.iss.shopsmart_backend.service.ProfileService;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CheckProfileExistsHandlerTest {

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private CheckProfileExistsHandler handler;

    @Mock
    private Handler nextHandler;

    @BeforeEach
    void setUp() {
        profileService = mock(ProfileService.class);
        nextHandler = mock(Handler.class);
        handler = new CheckProfileExistsHandler(profileService);
        handler.setNext(nextHandler);
    }

    @Test
    void testHandle_EmailIsEmpty() {
        ApiRequestResolver request = new ApiRequestResolver();
        request.addParamValue("email", ""); // Simulate empty email

        CompletableFuture<ApiResponseResolver> response = handler.handle(request, "admin");

        ApiResponseResolver result = response.join();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertEquals("Email is empty or not found in request for fetching user id", result.getRespData().get("message").asText());
    }

    @Test
    void testHandle_NoUserId_NeedsProfileFalse_WithNextHandler() {
        ApiRequestResolver request = new ApiRequestResolver();
        request.addParamValue("email", "user@domain.com");
        request.addParamValue("needsUserProf", "false");

        when(profileService.fetchUserIdForEmail(any(ApiRequestResolver.class), eq("user@domain.com"), eq("admin")))
                .thenReturn(CompletableFuture.completedFuture(null));

        ApiResponseResolver mockResponse = new ApiResponseResolver();
        mockResponse.setStatusCode(HttpStatus.OK);
        when(nextHandler.handle(any(ApiRequestResolver.class), eq("admin")))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        CompletableFuture<ApiResponseResolver> response = handler.handle(request, "admin");

        ApiResponseResolver result = response.join();
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(nextHandler, times(1)).handle(any(ApiRequestResolver.class), eq("admin"));
    }

    @Test
    void testHandle_NoUserId_NeedsProfileTrue() {
        ApiRequestResolver request = new ApiRequestResolver();
        request.addParamValue("email", "user@domain.com");
        request.addParamValue("needsUserProf", "true");

        when(profileService.fetchUserIdForEmail(any(ApiRequestResolver.class), eq("user@domain.com"), eq("admin")))
                .thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<ApiResponseResolver> response = handler.handle(request, "admin");

        ApiResponseResolver result = response.join();
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertEquals("No existing profile found for the email", result.getRespData().get("message").asText());
        verify(nextHandler, never()).handle(any(ApiRequestResolver.class), anyString());
    }

    @Test
    void testHandle_UserIdFound_WithNextHandler() {
        ApiRequestResolver request = new ApiRequestResolver();
        request.addParamValue("email", "user@domain.com");
        request.addParamValue("needsUserProf", "true");

        when(profileService.fetchUserIdForEmail(any(ApiRequestResolver.class), eq("user@domain.com"), eq("admin")))
                .thenReturn(CompletableFuture.completedFuture("user123"));

        ApiResponseResolver mockResponse = new ApiResponseResolver();
        mockResponse.setStatusCode(HttpStatus.OK);
        when(nextHandler.handle(any(ApiRequestResolver.class), eq("admin")))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        CompletableFuture<ApiResponseResolver> response = handler.handle(request, "admin");

        ApiResponseResolver result = response.join();
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(nextHandler, times(1)).handle(any(ApiRequestResolver.class), eq("admin"));
    }

    @Test
    void testHandle_UserIdFound_NoNextHandler() {
        ApiRequestResolver request = new ApiRequestResolver();
        request.addParamValue("email", "user@domain.com");
        request.addParamValue("needsUserProf", "false");

        handler.setNext(null); // Simulate no next handler

        when(profileService.fetchUserIdForEmail(any(ApiRequestResolver.class), eq("user@domain.com"), eq("admin")))
                .thenReturn(CompletableFuture.completedFuture("user123"));

        CompletableFuture<ApiResponseResolver> response = handler.handle(request, "admin");

        ApiResponseResolver result = response.join();
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("user123", result.getRespData().get("userId").asText());
    }
}
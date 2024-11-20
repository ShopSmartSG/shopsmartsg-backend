package sg.edu.nus.iss.shopsmart_backend.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.model.ApiResponseResolver;
import sg.edu.nus.iss.shopsmart_backend.service.ProfileService;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CreateProfileHandlerTest {
    private ProfileService profileService;
    private CreateProfileHandler handler;
    private Handler nextHandler;

    @BeforeEach
    void setUp() {
        profileService = mock(ProfileService.class);
        nextHandler = mock(Handler.class);
        handler = new CreateProfileHandler(profileService);
        handler.setNext(nextHandler);
    }

    @Test
    void testHandle_ProfileCreationFails() {
        ApiRequestResolver request = new ApiRequestResolver();

        when(profileService.createProfile(any(ApiRequestResolver.class), anyString()))
                .thenReturn(CompletableFuture.completedFuture(false)); // Simulate profile creation failure

        CompletableFuture<ApiResponseResolver> response = handler.handle(request, "admin");

        ApiResponseResolver result = response.join();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertEquals("Unable to create profile for the provided request", result.getRespData().get("message").asText());
        verify(nextHandler, never()).handle(any(ApiRequestResolver.class), anyString());
    }

    @Test
    void testHandle_ProfileCreationSuccess_WithNextHandler() {
        ApiRequestResolver request = new ApiRequestResolver();

        when(profileService.createProfile(any(ApiRequestResolver.class), anyString()))
                .thenReturn(CompletableFuture.completedFuture(true)); // Simulate profile creation success

        ApiResponseResolver mockResponse = new ApiResponseResolver();
        mockResponse.setStatusCode(HttpStatus.OK);
        when(nextHandler.handle(any(ApiRequestResolver.class), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        CompletableFuture<ApiResponseResolver> response = handler.handle(request, "admin");

        ApiResponseResolver result = response.join();
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(nextHandler, times(1)).handle(any(ApiRequestResolver.class), eq("admin"));
    }

    @Test
    void testHandle_ProfileCreationSuccess_NoNextHandler() {
        ApiRequestResolver request = new ApiRequestResolver();

        handler.setNext(null); // Simulate no next handler

        when(profileService.createProfile(any(ApiRequestResolver.class), anyString()))
                .thenReturn(CompletableFuture.completedFuture(true)); // Simulate profile creation success

        CompletableFuture<ApiResponseResolver> response = handler.handle(request, "admin");

        ApiResponseResolver result = response.join();
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Profile created successfully for user", result.getRespData().get("message").asText());
    }
}
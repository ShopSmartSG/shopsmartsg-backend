package sg.edu.nus.iss.shopsmart_backend.handlers;

import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.model.ApiResponseResolver;

import java.util.concurrent.CompletableFuture;

public interface Handler {
    void setNext(Handler nextHandler);
    CompletableFuture<ApiResponseResolver> handle(ApiRequestResolver request, String profileType);
}


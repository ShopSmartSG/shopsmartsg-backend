package sg.edu.nus.iss.shopsmart_backend.chains;

import sg.edu.nus.iss.shopsmart_backend.model.ApiRequestResolver;
import sg.edu.nus.iss.shopsmart_backend.model.ApiResponseResolver;

import java.util.concurrent.CompletableFuture;

public interface Chain {
    CompletableFuture<ApiResponseResolver> handleRequest(ApiRequestResolver request, String profileType);
}

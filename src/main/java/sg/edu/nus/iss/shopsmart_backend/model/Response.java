package sg.edu.nus.iss.shopsmart_backend.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.springframework.http.HttpStatusCode;

@Data
public class Response {
    private String status;
    private String message;
    private JsonNode data;
    private String errorMessage;
    private String errorCode;
    private HttpStatusCode httpStatusCode;
}

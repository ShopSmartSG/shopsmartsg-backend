package sg.edu.nus.iss.shopsmart_backend.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.springframework.http.HttpStatusCode;

@Data
public class ApiResponseResolver {
    HttpStatusCode statusCode;
    JsonNode respData;
}

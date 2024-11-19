package sg.edu.nus.iss.shopsmart_backend.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ApiRequestResolver {
    private String sessionId;
    private String correlationId;
    private String ipAddress;
    private String apiKey;
    private String additionalUriData;
    private String requestUri;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private Map<String, String> cookies;
    private Map<String, String> sessionAttributes;
    private JsonNode requestBody;
    private String loggerString;
    private Map<String, String> jwtClaims;
    private String userId;
    private String jwtToken;
    private boolean isLoggedIn;
    private Map<String, String> params;

    public void addParamValue(String key, String value) {
        if(params == null){
            params=new HashMap<>();
        }
        params.put(key, value);
    }

    public String getParamValue(String key) {
        if(params.containsKey(key)){
            return params.get(key);
        }
        return "";
    }
}

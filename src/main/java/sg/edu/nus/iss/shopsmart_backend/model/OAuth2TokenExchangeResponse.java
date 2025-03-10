package sg.edu.nus.iss.shopsmart_backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OAuth2TokenExchangeResponse {
    //actually is for access_token
    @JsonProperty("access_token")
    private String accessToken; //actual access token

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("scope")
    private String scopes;

    @JsonProperty("id_token")
    private String idToken;

    @JsonProperty("expires_in")
    private int expiresIn;

    private boolean invalidGrant;

//    {
//        "error": "invalid_grant",
//        "error_description": "Bad Request"
//    }
}

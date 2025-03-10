package sg.edu.nus.iss.shopsmart_backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GcipRefreshTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
    private String expiresIn;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("id_token")
    private String idToken;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("project_id")
    private String projectId;

    private boolean invalidRefreshToken;

//    {
//        "error": {
//            "code": 400,
//            "message": "INVALID_REFRESH_TOKEN",
//            "status": "INVALID_ARGUMENT"
//        }
//    }
}

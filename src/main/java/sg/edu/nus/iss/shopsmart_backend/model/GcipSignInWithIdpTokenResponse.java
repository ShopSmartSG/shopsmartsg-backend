package sg.edu.nus.iss.shopsmart_backend.model;

import lombok.Data;

@Data
public class GcipSignInWithIdpTokenResponse {
    private String federatedId;
    private String providerId;
    private String email;
    private boolean emailVerified;
    private String photoUrl;
    private String originalEmail;
    private String localId;
    private String idToken;
    private String oauthAccessToken;
    private int oauthExpireIn;
    private String refreshToken;
    private String expiresIn;
    private String rawUserInfo;
    private String kind;
    private boolean invalid;

//    {
//        "error": {
//            "code": 400,
//            "message": "INVALID_IDP_RESPONSE : Unsuccessful check authorization response from Google: {\n  \"error_description\": \"Invalid Value\"\n}\n",
//            "errors": [
//                {
//                    "message": "INVALID_IDP_RESPONSE : Unsuccessful check authorization response from Google: {\n  \"error_description\": \"Invalid Value\"\n}\n",
//                        "domain": "global",
//                        "reason": "invalid"
//                }
//            ]
//        }
//    }
}

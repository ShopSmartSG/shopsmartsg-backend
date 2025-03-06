package sg.edu.nus.iss.shopsmart_backend.model;

import lombok.Data;

@Data
public class GcipNativeLoginTokenResp {
    private String kind;
    private String localId;
    private String email;
    private String idToken;
    private boolean registered;
    private String refreshToken;
    private String expiresIn;
    private boolean invalidCredentials;

//    {
//        "error": {
//            "code": 400,
//            "message": "INVALID_LOGIN_CREDENTIALS",
//            "errors": [
//                {
//                    "message": "INVALID_LOGIN_CREDENTIALS",
//                        "domain": "global",
//                        "reason": "invalid"
//                }
//            ]
//        }
//    }
}

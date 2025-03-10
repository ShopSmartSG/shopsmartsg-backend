package sg.edu.nus.iss.shopsmart_backend.model;

import lombok.Data;

import java.util.List;

@Data
public class GcipAccLookupResponse {
    private String kind;
    private List<GcipAccLookupUser> users;
    private boolean idTokenRefreshNeeded;

//    {
//        "error": {
//            "code": 400,
//            "message": "INVALID_ID_TOKEN",
//            "errors": [
//                {
//                    "message": "INVALID_ID_TOKEN",
//                        "domain": "global",
//                        "reason": "invalid"
//                }
//            ]
//        }
//    }
}

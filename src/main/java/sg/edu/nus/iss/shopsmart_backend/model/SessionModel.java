package sg.edu.nus.iss.shopsmart_backend.model;

import lombok.Data;

@Data
public class SessionModel {
    String sessionId;
    String userId;
    long createdAt;
    long validTill;
    boolean isLoggedIn;

}

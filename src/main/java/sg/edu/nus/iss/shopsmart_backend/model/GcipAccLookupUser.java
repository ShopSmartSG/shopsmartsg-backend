package sg.edu.nus.iss.shopsmart_backend.model;

import lombok.Data;

import java.util.List;

@Data
public class GcipAccLookupUser {
    private String localId;
    private String photoUrl;
    private String email;
    private String passwordHash;
    private boolean emailVerified;
    private long passwordUpdatedAt;
    private List<GcipAccLookupUserProvider> providerUserInfo;
    private String validSince;
    private String lastLoginAt;
    private String createdAt;
    private String lastRefreshAt;
}

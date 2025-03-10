package sg.edu.nus.iss.shopsmart_backend.model;

import lombok.Data;

@Data
public class GcipAccLookupUserProvider {
    private String providerId;
    private String photoUrl;
    private String federatedId;
    private String email;
    private String rawId;
}

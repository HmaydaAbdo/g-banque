package staport.finance.gBanque.auth.dtos;

import java.util.List;

public class ProfileResponse {
    private String username;
    private List<String> roles;

    public ProfileResponse(String username, List<String> roles) {
        this.username = username;
        this.roles = roles;
    }


    public String getUsername() {
        return username;
    }

    public List<String> getRoles() {
        return roles;
    }
}
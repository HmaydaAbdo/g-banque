package staport.finance.gBanque.auth.dtos;

import java.util.List;

public class LoginResponse {
    private String accessToken;
    private String username;
    private List<String> authorities;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    public LoginResponse(String accessToken, String username, List<String> authorities) {
        this.accessToken = accessToken;
        this.username = username;
        this.authorities = authorities;
    }

    // Getter and Setter
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }
}
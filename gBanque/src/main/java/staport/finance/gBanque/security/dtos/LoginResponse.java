package staport.finance.gBanque.security.dtos;

public class LoginResponse {
    private String accessToken;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    // Getter and Setter
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
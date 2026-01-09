package staport.finance.gBanque.security.dtos;
/**
 * DTO for creating a new user
 */
public  class CreateUserRequest {
    private String username;
    private String password;
    private boolean enabled = true;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }


    @Override
    public String toString() {
        return "CreateUserRequest{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}

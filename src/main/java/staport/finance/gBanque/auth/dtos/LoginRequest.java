package staport.finance.gBanque.auth.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "Username cannot be empty")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Password cannot be empty")
        @Size(min = 5, message = "Password must be at least 5 characters long")
        String password
) {
}

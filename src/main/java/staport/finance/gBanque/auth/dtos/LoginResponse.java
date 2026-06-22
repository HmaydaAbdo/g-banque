package staport.finance.gBanque.auth.dtos;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String username,
        List<String> authorities
) {
}

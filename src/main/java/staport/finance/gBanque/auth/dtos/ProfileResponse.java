package staport.finance.gBanque.auth.dtos;

import java.util.List;

public record ProfileResponse(
        String username,
        List<String> roles
) {
}

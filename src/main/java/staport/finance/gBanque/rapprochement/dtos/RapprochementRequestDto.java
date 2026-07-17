package staport.finance.gBanque.rapprochement.dtos;

import jakarta.validation.constraints.NotNull;

public record RapprochementRequestDto(

        @NotNull(message = "id de la banque est obligatoire ")
        Long banqueId

) {}

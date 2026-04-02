package staport.finance.gBanque.transactions.dtos;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionUpdateRequestDto (

    @NotNull(message = "date de valeur est obligatoire")
    LocalDate dateValeur,

    @NotNull(message = "date d'opération est obligatoire")
    LocalDate dateOperation,

    @NotNull(message = "date de valeur est obligatoire")
    BigDecimal tvaDeductible,

    @NotNull(message = "date est obligatoire")
    BigDecimal tvaCollecte


){}

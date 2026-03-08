package staport.finance.gBanque.transactions.dtos;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public class TransactionUpdateRequestDto {

    @NotNull(message = "date de valeur est obligatoire")
    private OffsetDateTime dateValeur;

    public OffsetDateTime getDateValeur() {
        return dateValeur;
    }

    public void setDateValeur(OffsetDateTime dateValeur) {
        this.dateValeur = dateValeur;
    }

}

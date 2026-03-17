package staport.finance.gBanque.transactions.dtos;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class TransactionUpdateRequestDto {

    @NotNull(message = "date de valeur est obligatoire")
    private LocalDate dateValeur;

    public LocalDate getDateValeur() {
        return dateValeur;
    }

    public void setDateValeur(LocalDate dateValeur) {
        this.dateValeur = dateValeur;
    }

}

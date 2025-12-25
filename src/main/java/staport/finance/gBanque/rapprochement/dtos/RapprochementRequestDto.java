package staport.finance.gBanque.rapprochement.dtos;

import jakarta.validation.constraints.NotNull;

import java.time.YearMonth;

public class RapprochementRequestDto {

    @NotNull(message = "id de la banque est obligatoire ")
    private Long banqueId;

    @NotNull(message = "la date du rapprochement est obligatoire ")
    private YearMonth dateRapprochement;

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public YearMonth getDateRapprochement() {
        return dateRapprochement;
    }

    public void setDateRapprochement(YearMonth dateRapprochement) {
        this.dateRapprochement = dateRapprochement;
    }

    @Override
    public String toString() {
        return "RapprochementRequestDto{" +
                "banqueId=" + banqueId +
                ", dateRapprochement=" + dateRapprochement +
                '}';
    }
}

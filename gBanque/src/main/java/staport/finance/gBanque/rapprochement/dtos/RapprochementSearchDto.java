package staport.finance.gBanque.rapprochement.dtos;

import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;

public class RapprochementSearchDto {

    private Long banqueId;

    @DateTimeFormat(pattern = "MM/yyyy")
    private YearMonth dateRapprochement;

    // --- Constructeur ---
    public RapprochementSearchDto() {}

    // --- Getters et Setters ---

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
}
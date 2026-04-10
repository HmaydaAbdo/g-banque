package staport.finance.gBanque.rapprochement.dtos;

import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonSetter;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

public class RapprochementSearchDto {

    private Long banqueId;
    private YearMonth dateRapprochement;
    private StatutRapprochement statutRapprochement;

    public RapprochementSearchDto() {}

    public Long getBanqueId() { return banqueId; }
    public void setBanqueId(Long banqueId) { this.banqueId = banqueId; }

    public YearMonth getDateRapprochement() { return dateRapprochement; }

    // Accepte "1/2026" ET "01/2026"
    @JsonSetter("dateRapprochement")
    public void setDateRapprochement(String dateRapprochement) {
        if (dateRapprochement == null || dateRapprochement.isBlank()) {
            this.dateRapprochement = null;
            return;
        }
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.MONTH_OF_YEAR)   // 1 ou 2 chiffres
                .appendLiteral('/')
                .appendValue(ChronoField.YEAR, 4)
                .toFormatter();
        this.dateRapprochement = YearMonth.parse(dateRapprochement.trim(), formatter);
    }

    public StatutRapprochement getStatutRapprochement() {
        return statutRapprochement;
    }

    public void setStatutRapprochement(StatutRapprochement statutRapprochement) {
        this.statutRapprochement = statutRapprochement;
    }
}
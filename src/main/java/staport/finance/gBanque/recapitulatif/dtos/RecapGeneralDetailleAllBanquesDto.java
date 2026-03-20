package staport.finance.gBanque.recapitulatif.dtos;

import java.time.LocalDate;
import java.util.List;

public class RecapGeneralDetailleAllBanquesDto {

    private LocalDate dateRecap;
    private List<RecapGeneralDetailleDto> banques;

    public RecapGeneralDetailleAllBanquesDto() {
    }

    public LocalDate getDateRecap() {
        return dateRecap;
    }

    public void setDateRecap(LocalDate dateRecap) {
        this.dateRecap = dateRecap;
    }

    public List<RecapGeneralDetailleDto> getBanques() {
        return banques;
    }

    public void setBanques(List<RecapGeneralDetailleDto> banques) {
        this.banques = banques;
    }

    @Override
    public String toString() {
        return "RecapGeneralDetailleAllBanquesDto{" +
                "dateRecap=" + dateRecap +
                ", banques=" + banques +
                '}';
    }
}

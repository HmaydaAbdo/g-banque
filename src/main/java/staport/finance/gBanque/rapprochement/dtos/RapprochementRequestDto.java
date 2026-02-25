package staport.finance.gBanque.rapprochement.dtos;

import jakarta.validation.constraints.NotNull;

import java.time.YearMonth;

public class RapprochementRequestDto {

    @NotNull(message = "id de la banque est obligatoire ")
    private Long banqueId;



    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }



    @Override
    public String toString() {
        return "RapprochementRequestDto{" +
                "banqueId=" + banqueId +
                '}';
    }
}

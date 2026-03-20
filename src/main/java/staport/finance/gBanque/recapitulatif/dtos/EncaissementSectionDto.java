package staport.finance.gBanque.recapitulatif.dtos;

import java.math.BigDecimal;
import java.util.List;

public class EncaissementSectionDto {
    private List<CategorieAmountDto> categories;
    private BigDecimal total;

    public EncaissementSectionDto() {
    }

    public List<CategorieAmountDto> getCategories() {
        return categories;
    }

    public void setCategories(List<CategorieAmountDto> categories) {
        this.categories = categories;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    @Override
    public String toString() {
        return "EncaissementSectionDto{" +
                "categories=" + categories +
                ", total=" + total +
                '}';
    }
}

package staport.finance.gBanque.recapitulatif.dtos;

import java.math.BigDecimal;
import java.util.List;

public class DecaissementSectionDto {
    private List<CategorieAmountDto> categories;   // une ligne par module
    private BigDecimal total;

    public DecaissementSectionDto() {
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
        return "DecaissementSectionDto{" +
                "categories=" + categories +
                ", total=" + total +
                '}';
    }
}
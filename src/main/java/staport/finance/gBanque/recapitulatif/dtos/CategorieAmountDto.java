package staport.finance.gBanque.recapitulatif.dtos;

import java.math.BigDecimal;

public class CategorieAmountDto {
    private String categorie;       // "Chèque", "LCN", "Virement", etc.
    private BigDecimal montant;

    public CategorieAmountDto() {
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    @Override
    public String toString() {
        return "CategorieAmountDto{" +
                "categorie='" + categorie + '\'' +
                ", montant=" + montant +
                '}';
    }
}
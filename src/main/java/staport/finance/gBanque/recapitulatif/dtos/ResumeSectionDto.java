package staport.finance.gBanque.recapitulatif.dtos;

import java.math.BigDecimal;

public class ResumeSectionDto {

    private BigDecimal soldeActuel;
    private BigDecimal plafondLigne;
    private BigDecimal disponibilite;
    private BigDecimal encoursPlus;
    private BigDecimal encoursMoins;
    private BigDecimal soldeProjete; // disponibilité + encousPlus - encoursMoins

    public ResumeSectionDto() {
    }

    public BigDecimal getSoldeActuel() {
        return soldeActuel;
    }

    public void setSoldeActuel(BigDecimal soldeActuel) {
        this.soldeActuel = soldeActuel;
    }

    public BigDecimal getPlafondLigne() {
        return plafondLigne;
    }

    public void setPlafondLigne(BigDecimal plafondLigne) {
        this.plafondLigne = plafondLigne;
    }

    public BigDecimal getDisponibilite() {
        return disponibilite;
    }

    public void setDisponibilite(BigDecimal disponibilite) {
        this.disponibilite = disponibilite;
    }

    public BigDecimal getEncoursPlus() {
        return encoursPlus;
    }

    public void setEncoursPlus(BigDecimal encoursPlus) {
        this.encoursPlus = encoursPlus;
    }

    public BigDecimal getEncoursMoins() {
        return encoursMoins;
    }

    public void setEncoursMoins(BigDecimal encoursMoins) {
        this.encoursMoins = encoursMoins;
    }

    public BigDecimal getSoldeProjete() {
        return soldeProjete;
    }

    public void setSoldeProjete(BigDecimal soldeProjete) {
        this.soldeProjete = soldeProjete;
    }

    @Override
    public String toString() {
        return "ResumeSectionDto{" +
                "soldeActuel=" + soldeActuel +
                ", plafondLigne=" + plafondLigne +
                ", disponibilite=" + disponibilite +
                ", encoursPlus=" + encoursPlus +
                ", encoursMoins=" + encoursMoins +
                ", soldeProjete=" + soldeProjete +
                '}';
    }
}
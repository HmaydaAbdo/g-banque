package staport.finance.gBanque.miseADisposition.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class EncoursMiseAdispositionSearchDto {

    private String numero;
    private String beneficiaire;
    private BigDecimal montant;
    private String emetteur;
    private OffsetDateTime dateEmission;

    public EncoursMiseAdispositionSearchDto() {
    }

    public String getEmetteur() {
        return emetteur;
    }

    public void setEmetteur(String emetteur) {
        this.emetteur = emetteur;
    }

    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(OffsetDateTime dateEmission) {
        this.dateEmission = dateEmission;
    }

    public String getNumero() {
        return numero;
    }
    public void setNumero(String numero) {
        this.numero = numero;
    }
    public String getBeneficiaire() {
        return beneficiaire;
    }
    public void setBeneficiaire(String beneficiaire) {
        this.beneficiaire = beneficiaire;
    }

    public BigDecimal getMontant() {
        return montant;
    }
    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    @Override
    public String toString() {
        return "EncoursMiseAdispositionSearchDto{" +
                "numero='" + numero + '\'' +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", montant=" + montant +
                ", emetteur='" + emetteur + '\'' +
                ", dateEmission=" + dateEmission +
                '}';
    }
}

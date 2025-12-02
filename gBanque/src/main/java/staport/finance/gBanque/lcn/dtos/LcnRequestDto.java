package staport.finance.gBanque.lcn.dtos;

import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.virements.enums.Type;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class LcnRequestDto {

    @NotNull(message = "le numero est obligatoire")
    private String numero;

    @NotNull(message = "la date d'émission est obligatoire ")
    private OffsetDateTime dateEmission;

    @NotNull(message = "la date d'échéance  est obligatoire ")
    private OffsetDateTime dateEcheance;

    @NotNull(message = "le beneficiaire est obligatoire ")
    private final String beneficiaire="STAPORT";

    @NotNull(message = "l'id de la banque est obligatoire ")
    private Long banqueId;

    @NotNull(message = "l'emetteur est pbligatoire")
    private String emetteur;

    @NotNull(message = "le montant est obligatoire")
    private BigDecimal montant;

    @NotNull(message = "le type est encaissement est doit etre egal à encaissement")
    private final Type type=Type.ENCAISSEMENT;


    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(OffsetDateTime dateEmission) {
        this.dateEmission = dateEmission;
    }

    public OffsetDateTime getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(OffsetDateTime dateEcheance) {
        this.dateEcheance = dateEcheance;
    }

    public String getBeneficiaire() {
        return beneficiaire;
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public String getEmetteur() {
        return emetteur;
    }

    public void setEmetteur(String emetteur) {
        this.emetteur = emetteur;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public Type getType() {
        return type;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    @Override
    public String toString() {
        return "LcnRequestDto{" +
                "numero='" + numero + '\'' +
                ", dateEmission=" + dateEmission +
                ", dateEcheance=" + dateEcheance +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", banqueId=" + banqueId +
                ", emetteur='" + emetteur + '\'' +
                ", montant=" + montant +
                ", type=" + type +
                '}';
    }
}

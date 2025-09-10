package staport.finance.gBanque.marches.dtos;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class MarcheSearchDTO {

    @Size(max = 255, message = "Le nom doit avoir au maximum 255 caracteres")
    private String nom;

    @Size(max = 50, message = "Le numero doit avoir au maximum 50 caracteres")
    private String numero;

    @DecimalMin(value = "0.00", message = "Le montant minimum doit etre superieur ou egal a zero")
    private BigDecimal minMontant;

    @DecimalMin(value = "0.00", message = "Le montant maximum doit etre superieur ou egal a zero")
    private BigDecimal maxMontant;

    @Size(max = 255, message = "Le maitre d'ouvrage doit avoir au maximum 255 caracteres")
    private String maitreDouvrage;

    private OffsetDateTime delaiBefore;

    // Getters and Setters (omitted for brevity)
    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public BigDecimal getMinMontant() {
        return minMontant;
    }

    public void setMinMontant(BigDecimal minMontant) {
        this.minMontant = minMontant;
    }

    public BigDecimal getMaxMontant() {
        return maxMontant;
    }

    public void setMaxMontant(BigDecimal maxMontant) {
        this.maxMontant = maxMontant;
    }

    public String getMaitreDouvrage() {
        return maitreDouvrage;
    }

    public void setMaitreDouvrage(String maitreDouvrage) {
        this.maitreDouvrage = maitreDouvrage;
    }

    public OffsetDateTime getDelaiBefore() {
        return delaiBefore;
    }

    public void setDelaiBefore(OffsetDateTime delaiBefore) {
        this.delaiBefore = delaiBefore;
    }
}
package staport.finance.gBanque.LigneFonctionnelle.dtos;


import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class SousLigneRequestDto {

    private Long id;
    // Le plafond est facultatif pour une Sous-ligne.
    @DecimalMin(value = "0.0", inclusive = false, message = "Le plafond doit être supérieur à zéro.")
    private BigDecimal plafond;

    // L'ID de la nature de ligne est obligatoire, même pour une sous-ligne si elle peut différer.
    @NotNull(message = "L'ID de la nature de ligne fonctionnelle est obligatoire pour la sous-ligne.")
    private Long natureLigneFonctionnelleId;

    // --- Constructors, Getters, Setters ---
    public SousLigneRequestDto() {}

    public BigDecimal getPlafond() { return plafond; }
    public void setPlafond(BigDecimal plafond) { this.plafond = plafond; }

    public Long getNatureLigneFonctionnelleId() { return natureLigneFonctionnelleId; }
    public void setNatureLigneFonctionnelleId(Long natureLigneFonctionnelleId) { this.natureLigneFonctionnelleId = natureLigneFonctionnelleId; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Override
    public String toString() {
        return "SousLigneRequestDto{" +
                "id=" + id +
                ", plafond=" + plafond +
                ", natureLigneFonctionnelleId=" + natureLigneFonctionnelleId +
                '}';
    }
}
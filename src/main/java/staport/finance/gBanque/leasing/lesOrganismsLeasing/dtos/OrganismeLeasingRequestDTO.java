package staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class OrganismeLeasingRequestDTO {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotNull(message = "Le plafond est obligatoire")
    private BigDecimal plafond;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut ;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate dateFin ;


    @AssertTrue(message = "La date de fin doit être postérieure à la date de début")
    public boolean isDateFinApresDateDebut() {
        return dateDebut != null && dateFin != null && dateFin.isAfter(dateDebut);
    }

    // Getters and Setters
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public BigDecimal getPlafond() { return plafond; }
    public void setPlafond(BigDecimal plafond) { this.plafond = plafond; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    @Override
    public String toString() {
        return "OrganismeLeasingRequestDTO{" +
                "nom='" + nom + '\'' +
                ", plafond=" + plafond +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                '}';
    }
}
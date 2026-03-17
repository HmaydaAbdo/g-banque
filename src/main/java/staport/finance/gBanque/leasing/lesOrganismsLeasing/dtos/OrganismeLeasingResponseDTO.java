package staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public class OrganismeLeasingResponseDTO {

    private Long id;
    private String nom;
    private BigDecimal plafond;
    private LocalDate dateDebut ;
    private LocalDate dateFin ;


    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public BigDecimal getPlafond() { return plafond; }
    public void setPlafond(BigDecimal plafond) { this.plafond = plafond; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }


}
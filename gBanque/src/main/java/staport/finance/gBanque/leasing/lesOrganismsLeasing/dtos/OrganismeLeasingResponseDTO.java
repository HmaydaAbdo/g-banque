package staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class OrganismeLeasingResponseDTO {

    private Long id;
    private String nom;
    private BigDecimal plafond;
    private OffsetDateTime dateDebut;
    private OffsetDateTime dateFin;


    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public BigDecimal getPlafond() { return plafond; }
    public void setPlafond(BigDecimal plafond) { this.plafond = plafond; }

    public OffsetDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(OffsetDateTime dateDebut) { this.dateDebut = dateDebut; }

    public OffsetDateTime getDateFin() { return dateFin; }
    public void setDateFin(OffsetDateTime dateFin) { this.dateFin = dateFin; }


}
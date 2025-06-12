
package staport.finance.gBanque.Bank.dtos;

import staport.finance.gBanque.Bank.enums.Devise;
import java.math.BigDecimal;
import java.time.OffsetDateTime;


public class BanqueResponse {

    private Long id;
    private String logoBase64;
    private String nom;
    private String numeroDeCompte;
    private Devise devise;
    private String address;
    private String agence;
    private BigDecimal soldeActuel;

    private OffsetDateTime dateCreation;
    private OffsetDateTime dateModification;

    // --- Getters and Setters (no Lombok) ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLogoBase64() { return logoBase64; }
    public void setLogoBase64(String logoBase64) { this.logoBase64 = logoBase64; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getNumeroDeCompte() { return numeroDeCompte; }
    public void setNumeroDeCompte(String numeroDeCompte) { this.numeroDeCompte = numeroDeCompte; }
    public Devise getDevise() { return devise; }
    public void setDevise(Devise devise) { this.devise = devise; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getAgence() { return agence; }
    public void setAgence(String agence) { this.agence = agence; }
    public BigDecimal getSoldeActuel() { return soldeActuel; }
    public void setSoldeActuel(BigDecimal soldeActuel) { this.soldeActuel = soldeActuel; }
    public OffsetDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(OffsetDateTime dateCreation) { this.dateCreation = dateCreation; }
    public OffsetDateTime getDateModification() { return dateModification; }
    public void setDateModification(OffsetDateTime dateModification) { this.dateModification = dateModification; }
}
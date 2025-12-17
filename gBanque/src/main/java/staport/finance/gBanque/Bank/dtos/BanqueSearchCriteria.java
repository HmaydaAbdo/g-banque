package staport.finance.gBanque.Bank.dtos;

import java.math.BigDecimal;

public class BanqueSearchCriteria {
    private String nom;
    private String numeroDeCompte;
    private BigDecimal minSolde;
    private BigDecimal maxSolde;


    // --- Getters and Setters (no Lombok) ---
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getNumeroDeCompte() { return numeroDeCompte; }
    public void setNumeroDeCompte(String numeroDeCompte) { this.numeroDeCompte = numeroDeCompte; }
    public BigDecimal getMinSolde() { return minSolde; }
    public void setMinSolde(BigDecimal minSolde) { this.minSolde = minSolde; }
    public BigDecimal getMaxSolde() { return maxSolde; }
    public void setMaxSolde(BigDecimal maxSolde) { this.maxSolde = maxSolde; }

    @Override
    public String toString() {
        return "BanqueSearchCriteria{" +
                "nom='" + nom + '\'' +
                ", numeroDeCompte='" + numeroDeCompte + '\'' +
                ", minSolde=" + minSolde +
                ", maxSolde=" + maxSolde +
                '}';
    }
}
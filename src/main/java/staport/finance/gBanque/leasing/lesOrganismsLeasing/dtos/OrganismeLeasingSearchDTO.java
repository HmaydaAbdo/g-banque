package staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos;

public class OrganismeLeasingSearchDTO {

    private String nom;

    // Getters and Setters
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    @Override
    public String toString() {
        return "OrganismeLeasingSearchDTO{" +
                "nom='" + nom + '\'' +
                '}';
    }
}
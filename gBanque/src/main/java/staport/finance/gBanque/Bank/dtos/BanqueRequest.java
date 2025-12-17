package staport.finance.gBanque.Bank.dtos;

import jakarta.validation.constraints.*;
import staport.finance.gBanque.Bank.enums.Devise;


public class BanqueRequest {



    @NotBlank(message = "Le nom de la banque est obligatoire.")
    @Pattern(regexp = "^[a-zA-Z\\s]*$", message = "Le nom de la banque ne doit contenir que des caractères alphabétiques et des espaces.")
    @Size(min = 2, max = 100, message = "Le nom de la banque doit avoir entre 2 et 100 caractères.")
    private String nom;

    @NotBlank(message = "Le numéro de compte est obligatoire.")
    @Size(min = 24, max = 24, message = "Le numéro de compte doit comporter exactement 24 chiffres.")
    @Pattern(regexp = "^[0-9]*$", message = "Le numéro de compte ne doit contenir que des chiffres.")
    private String numeroDeCompte;

    @NotNull(message = "La devise est obligatoire.")
    private Devise devise;

    @NotBlank(message = "L'adresse est obligatoire.")
    @Size(min = 5, max = 255, message = "L'adresse doit avoir entre 5 et 255 caractères.")
    private String address;

    @NotBlank(message = "L'agence est obligatoire.")
    @Size(min = 2, max = 100, message = "L'agence doit avoir entre 2 et 100 caractères.")
    private String agence;




    // --- Getters and Setters (no Lombok) ---

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

    @Override
    public String toString() {
        return "BanqueRequest{" +
                "nom='" + nom + '\'' +
                ", numeroDeCompte='" + numeroDeCompte + '\'' +
                ", devise=" + devise +
                ", address='" + address + '\'' +
                ", agence='" + agence + '\'' +
                '}';
    }
}
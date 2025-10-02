package staport.finance.gBanque.marches.dtos;



import jakarta.validation.constraints.Size;


public class MarcheSearchDTO {

    @Size(max = 255, message = "Le nom doit avoir au maximum 255 caracteres")
    private String nom;

    @Size(max = 50, message = "Le numero doit avoir au maximum 50 caracteres")
    private String numero;

    @Size(max = 255, message = "Le maitre d'ouvrage doit avoir au maximum 255 caracteres")
    private String maitreDouvrage;

    private Long banqueId;


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




    public String getMaitreDouvrage() {
        return maitreDouvrage;
    }

    public void setMaitreDouvrage(String maitreDouvrage) {
        this.maitreDouvrage = maitreDouvrage;
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }
}
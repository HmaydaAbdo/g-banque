package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos;


import jakarta.validation.constraints.NotBlank;


public class NatureLigneFonctionnelleRequest {

    @NotBlank(message = "Le label de la nature de ligne fonctionnelle est obligatoire.")
    private String label;

    @NotBlank(message = "La description de la nature de ligne fonctionnelle est obligatoire.")
    private String description;

    // --- Constructors ---
    public NatureLigneFonctionnelleRequest() {
    }

    public NatureLigneFonctionnelleRequest(String label, String description) {
        this.label = label;
        this.description = description;
    }

    // --- Getters and Setters ---
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}



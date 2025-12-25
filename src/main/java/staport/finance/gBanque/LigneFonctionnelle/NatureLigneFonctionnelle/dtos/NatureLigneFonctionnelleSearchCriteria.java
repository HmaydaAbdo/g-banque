package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos;


public class NatureLigneFonctionnelleSearchCriteria {
    private String label;
    private String description;

    // --- Constructors ---
    public NatureLigneFonctionnelleSearchCriteria() {
    }

    public NatureLigneFonctionnelleSearchCriteria(String label, String description) {
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
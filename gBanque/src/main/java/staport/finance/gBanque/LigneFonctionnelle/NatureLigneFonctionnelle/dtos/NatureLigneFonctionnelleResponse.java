package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos;


import java.time.OffsetDateTime;

public class NatureLigneFonctionnelleResponse {

    private Long id;
    private String label;
    private String description;
    private OffsetDateTime dateCreation;
    private OffsetDateTime dateModification;

    // --- Constructors ---
    public NatureLigneFonctionnelleResponse() {
    }

    public NatureLigneFonctionnelleResponse(Long id, String label, String description, OffsetDateTime dateCreation, OffsetDateTime dateModification) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.dateCreation = dateCreation;
        this.dateModification = dateModification;
    }

    // --- Getters and Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public OffsetDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(OffsetDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public OffsetDateTime getDateModification() {
        return dateModification;
    }

    public void setDateModification(OffsetDateTime dateModification) {
        this.dateModification = dateModification;
    }
}

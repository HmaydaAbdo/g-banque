package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "nature_ligne_fonctionnelle")
public class NatureLigneFonctionnelle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String label;

    @Column(nullable = false)
    private String description;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;

    // --- Constructors ---
    public NatureLigneFonctionnelle() {
        // Default constructor required by JPA
    }

    public NatureLigneFonctionnelle(Long id, String label, String description, OffsetDateTime dateCreation, OffsetDateTime dateModification) {
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

    // --- JPA Lifecycle Callbacks ---
    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.dateCreation = now;
        this.dateModification = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = OffsetDateTime.now();
    }

    // --- equals() and hashCode() ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NatureLigneFonctionnelle that = (NatureLigneFonctionnelle) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // --- toString() ---
    @Override
    public String toString() {
        return "NatureLigneFonctionnelle{" +
                "id=" + id +
                ", label='" + label + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
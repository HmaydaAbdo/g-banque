package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities;
import jakarta.persistence.*;
import jakarta.persistence.Index;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDate;
import java.util.Objects;

@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(
    name = "nature_ligne_fonctionnelle",
    indexes = {
        @Index(name = "idx_nlf_code",  columnList = "code"),
        @Index(name = "idx_nlf_label", columnList = "label")
    }
)
public class NatureLigneFonctionnelle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20, updatable = false)
    private String code;        // Immutable system identifier

    @Column(nullable = false, unique = true)
    private String label;

    @Column(nullable = false)
    private String description;

    @CreatedDate
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDate dateCreation;

    @LastModifiedDate
    @Column(name = "date_modification", nullable = false)
    private LocalDate dateModification;

    // --- Constructors ---
    public NatureLigneFonctionnelle() {
        // Default constructor required by JPA
    }

    public NatureLigneFonctionnelle(Long id, String code, String label, String description, LocalDate dateCreation, LocalDate dateModification) {
        this.id = id;
        this.code = code;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;   // Used only by the mapper at creation time. Column is updatable = false in JPA.
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

    public LocalDate getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDate dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDate getDateModification() {
        return dateModification;
    }

    public void setDateModification(LocalDate dateModification) {
        this.dateModification = dateModification;
    }



    // --- equals() and hashCode() ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NatureLigneFonctionnelle that = (NatureLigneFonctionnelle) o;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    // --- toString() ---
    @Override
    public String toString() {
        return "NatureLigneFonctionnelle{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", label='" + label + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
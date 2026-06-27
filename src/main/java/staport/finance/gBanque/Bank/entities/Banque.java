package staport.finance.gBanque.Bank.entities;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import staport.finance.gBanque.Bank.enums.Devise;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Entité sensible (compte bancaire) — auditée via Hibernate Envers (Backend/CLAUDE.md,
 * section "Audit"). {@code logoData} exclu via {@code @NotAudited} : un blob binaire
 * dans chaque révision serait inutile à differ et gonflerait inutilement la table _AUD.
 */
@EntityListeners(AuditingEntityListener.class)
@Entity
@Audited
@Table(
    name = "banque",
    indexes = {
        @Index(name = "idx_banque_numero_compte", columnList = "numero_compte"),
        @Index(name = "idx_banque_nom",           columnList = "nom"),
        @Index(name = "idx_banque_devise",         columnList = "devise")
    }
)
public class Banque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotAudited
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "logo_data", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] logoData;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nom;

    @NotBlank
    @Pattern(regexp = "\\d{24}")
    @Column(name = "numero_compte", nullable = false, unique = true, length = 24)
    private String numeroDeCompte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Devise devise;

    @NotBlank
    @Column(name = "address", nullable = false, length = 255)
    private String adresse;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String agence;

    @CreatedDate
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDate dateCreation;

    @LastModifiedDate
    @Column(name = "date_modification", nullable = false)
    private LocalDate dateModification;

    // --- Constructors ---
    public Banque() {}

    // --- Getters / Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public byte[] getLogoData() { return logoData; }
    public void setLogoData(byte[] logoData) { this.logoData = logoData; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getNumeroDeCompte() { return numeroDeCompte; }
    public void setNumeroDeCompte(String numeroDeCompte) { this.numeroDeCompte = numeroDeCompte; }

    public Devise getDevise() { return devise; }
    public void setDevise(Devise devise) { this.devise = devise; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getAgence() { return agence; }
    public void setAgence(String agence) { this.agence = agence; }

    public LocalDate getDateCreation() { return dateCreation; }
    public LocalDate getDateModification() { return dateModification; }


    // --- equals / hashCode ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Banque banque = (Banque) o;
        return Objects.equals(numeroDeCompte, banque.numeroDeCompte);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numeroDeCompte);
    }

    // --- toString ---

    @Override
    public String toString() {
        return "Banque{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", numeroDeCompte='" + numeroDeCompte + '\'' +
                ", devise=" + devise +
                ", adresse='" + adresse + '\'' +
                ", agence='" + agence + '\'' +
                ", dateModification=" + dateModification +
                '}';
    }
}

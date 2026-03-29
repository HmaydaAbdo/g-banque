package staport.finance.gBanque.Bank.entities;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import staport.finance.gBanque.Bank.enums.Devise;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@EntityListeners(AuditingEntityListener.class)
@Entity
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

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "logo_data", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] logoData;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String nom;

    @NotBlank
    @Pattern(regexp = "\\d{24}")
    @Column(name = "numero_compte", nullable = false, unique = true, length = 24)
    private String numeroDeCompte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Devise devise;

    @NotBlank
    @Column(nullable = false)
    private String address;

    @NotBlank
    @Column(nullable = false)
    private String agence;

    @Column(name = "solde_depart", nullable = false)
    private BigDecimal soldeDepart = BigDecimal.ZERO;

    /**
     * @deprecated soldeActuel is still persisted for backward compatibility with out-of-scope modules
     * (ValidationService, ReverseTransactionService, RecapGeneralDetailleService, etc.).
     * New code must use {@code BanqueService.computeLiveBalance()} instead.
     * Migration to computed balance is tracked in a dedicated future session.
     */
    @Deprecated
    @Column(name = "solde_actuel")
    private BigDecimal soldeActuel = BigDecimal.ZERO;

    @CreatedDate
    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    @LastModifiedDate
    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;

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

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getAgence() { return agence; }
    public void setAgence(String agence) { this.agence = agence; }

    public BigDecimal getSoldeDepart() {
        return soldeDepart;
    }
    public void setSoldeDepart(BigDecimal soldeDepart) {
        this.soldeDepart = soldeDepart;
    }

    /** @deprecated Use {@code BanqueService.computeLiveBalance()} for accurate balance. */
    @Deprecated
    public BigDecimal getSoldeActuel() {
        return soldeActuel;
    }

    /** @deprecated Use {@code BanqueService.computeLiveBalance()} for accurate balance. */
    @Deprecated
    public void setSoldeActuel(BigDecimal soldeActuel) {
        this.soldeActuel = soldeActuel;
    }

    public OffsetDateTime getDateCreation() { return dateCreation; }
    public OffsetDateTime getDateModification() { return dateModification; }


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
                ", agence='" + agence + '\'' +
                ", dateModification=" + dateModification +
                '}';
    }
}

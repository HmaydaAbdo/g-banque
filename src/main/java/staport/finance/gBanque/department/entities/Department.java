package staport.finance.gBanque.department.entities;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;

import java.time.Instant;

/**
 * Représente un département métier (Trésorerie, Crédits, Paiements, Comptabilité, Direction...).
 *
 * <p>Les départements sont entièrement administrables via l'API/UI — pas d'enum Java —
 * afin que l'administrateur système puisse en créer, renommer ou supprimer sans déploiement.
 *
 * <p>Le nom du département (ex. "TRESORERIE") sert de clé pour construire les authorities
 * Spring Security : {@code DEPT_<name>:MEMBER} et {@code DEPT_<name>:DIRECTOR}.
 * Il est donc recommandé d'utiliser une convention stable (majuscules, sans espace) côté admin.
 *
 * <p>Entité RBAC sensible — auditée via Hibernate Envers (Backend/CLAUDE.md, section "Audit").
 */
@Entity
@Audited
@Table(
        name = "department",
        indexes = {
                @Index(name = "idx_department_name", columnList = "name")
        }
)
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ========== CONSTRUCTORS ==========

    public Department() {
    }

    public Department(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // ========== GETTERS ==========

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // ========== SETTERS ==========

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // ========== EQUALS / HASHCODE ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Department that = (Department) o;
        return id != null && id.equals
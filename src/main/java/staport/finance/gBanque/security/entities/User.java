package staport.finance.gBanque.security.entities;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    private String password;

    private boolean enabled;

    /**
     * Administrateur système : accès transversal à tous les départements, n'appartient
     * lui-même à aucun département. Distinct du modèle MEMBER/DIRECTOR par département.
     */
    @Column(name = "is_system_admin", nullable = false)
    private boolean isSystemAdmin = false;

    /**
     * Appartenances de cet utilisateur aux différents départements, avec son rôle
     * (MEMBER ou DIRECTOR) dans chacun. Un utilisateur peut appartenir à plusieurs
     * départements simultanément.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UserDepartmentRole> departmentRoles = new ArrayList<>();

    // ========== CONSTRUCTORS ==========
    public User() {
    }

    public User(String username, String password, boolean enabled) {
        this.username = username;
        this.password = password;
        this.enabled = enabled;
    }

    // ========== GETTERS ==========
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isSystemAdmin() {
        return isSystemAdmin;
    }

    public List<UserDepartmentRole> getDepartmentRoles() {
        return departmentRoles;
    }

    // ========== SETTERS (ADDED) ==========
    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setSystemAdmin(boolean systemAdmin) {
        this.isSystemAdmin = systemAdmin;
    }

    public void setDepartmentRoles(List<UserDepartmentRole> departmentRoles) {
        this.departmentRoles = departmentRoles;
    }
}
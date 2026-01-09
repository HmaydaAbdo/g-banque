package staport.finance.gBanque.security.entities;
import jakarta.persistence.*;
import staport.finance.gBanque.security.enums.RoleName;


import jakarta.persistence.*;
import staport.finance.gBanque.security.enums.RoleName;

@Entity
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private RoleName roleName;

    // ========== CONSTRUCTORS ==========
    public Role() {
    }

    public Role(RoleName roleName) {
        this.roleName = roleName;
    }

    // ========== GETTERS ==========
    public Long getId() {
        return id;
    }

    public RoleName getRoleName() {
        return roleName;
    }

    // ========== SETTERS (ADDED) ==========
    public void setId(Long id) {
        this.id = id;
    }

    public void setRoleName(RoleName roleName) {
        this.roleName = roleName;
    }

    // ========== EQUALS AND HASHCODE (IMPORTANT FOR COLLECTIONS) ==========
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return roleName == role.roleName;
    }

    @Override
    public int hashCode() {
        return roleName != null ? roleName.hashCode() : 0;
    }
}
package staport.finance.gBanque.user.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import staport.finance.gBanque.department.entities.Department;
import staport.finance.gBanque.user.enums.DepartmentRoleType;

/**
 * Table pivot reliant un {@link User} à un {@link Department} avec un rôle
 * ({@link DepartmentRoleType#MEMBER} ou {@link DepartmentRoleType#DIRECTOR}).
 *
 * <p>Un utilisateur peut avoir plusieurs lignes — une par département auquel il appartient —
 * mais une seule ligne par département (contrainte unique {@code user_id + department_id}).
 * Cela permet le cas "utilisateur multi-département" : MEMBER dans TRESORERIE et DIRECTOR
 * dans CREDITS en même temps, par exemple.
 *
 * <p>Chaque ligne se traduit en une authority Spring Security lors de la génération du JWT :
 * {@code DEPT_<department.name>:<roleInDept>} (ex. {@code DEPT_TRESORERIE:DIRECTOR}).
 */
@Entity
@Table(
        name = "user_department_role",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_dept",
                columnNames = {"user_id", "department_id"}
        ),
        indexes = {
                @Index(name = "idx_user_department_role_department_id", columnList = "department_id")
        }
)
public class UserDepartmentRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence inverse — exclue de la sérialisation JSON pour éviter le cycle
     * User.departmentRoles → UserDepartmentRole.user → (même) User → ...
     * Le JSON exposé via /users/{id}/department-roles n'a pas besoin de réafficher
     * l'utilisateur : il est déjà connu via l'URL appelante.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_in_dept", nullable = false, length = 20)
    private DepartmentRoleType roleInDept;

    // ========== CONSTRUCTORS ==========

    public UserDepartmentRole() {
    }

    public UserDepartmentRole(User user, Department department, DepartmentRoleType roleInDept) {
        this.user = user;
        this.department = department;
        this.roleInDept = roleInDept;
    }

    // ========== GETTERS ==========

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Department getDepartment() {
        return department;
    }

    public DepartmentRoleType getRoleInDept() {
        return roleInDept;
    }

    // ========== SETTERS ==========

    public void setId(Long id) {
        this.id = id;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public void setRoleInDept(DepartmentRoleType roleInDept) {
        this.roleInDept = roleInDept;
    }

    // ========== EQUALS / HASHCODE ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDepartmentRole that = (UserDepartmentRole) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}

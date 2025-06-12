package staport.finance.gBanque.security.entities;
import jakarta.persistence.*;
import staport.finance.gBanque.security.enums.RoleName;

@Entity
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Role(RoleName roleName) {
        this.roleName = roleName;
    }

    @Enumerated(EnumType.STRING)
    private RoleName roleName;

    public Role() {
    }

    public RoleName getRoleName() {
        return roleName;
    }
}
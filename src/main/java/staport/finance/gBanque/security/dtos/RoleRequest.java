package staport.finance.gBanque.security.dtos;
/**
 * DTO for role operations
 */
public  class RoleRequest {
    private Long roleId;
    private String roleName;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    @Override
    public String toString() {
        return "RoleRequest{" +
                "roleId=" + roleId +
                ", roleName='" + roleName + '\'' +
                '}';
    }
}
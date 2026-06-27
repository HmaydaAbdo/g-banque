package staport.finance.gBanque.auth.services;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import staport.finance.gBanque.user.entities.User;
import staport.finance.gBanque.user.entities.UserDepartmentRole;
import staport.finance.gBanque.user.services.UserService;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;

    public UserDetailsServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User appUser = userService.findByUsername(username);

        return new org.springframework.security.core.userdetails.User(
                appUser.getUsername(),
                appUser.getPassword(),
                appUser.isEnabled(),
                true, true, true,
                buildAuthorities(appUser)
        );
    }

    /**
     * Construit les authorities Spring Security à partir du modèle RBAC département :
     * <ul>
     *   <li>{@code SYSTEM_ADMIN} si {@link User#isSystemAdmin()} — accès transversal,
     *       l'utilisateur n'appartient lui-même à aucun département.</li>
     *   <li>{@code DEPT_<nom du département>:<MEMBER|DIRECTOR>} pour chaque appartenance
     *       de l'utilisateur — un utilisateur peut appartenir à plusieurs départements.</li>
     * </ul>
     */
    private List<GrantedAuthority> buildAuthorities(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        if (user.isSystemAdmin()) {
            authorities.add(new SimpleGrantedAuthority("SYSTEM_ADMIN"));
        }

        for (UserDepartmentRole udr : user.getDepartmentRoles()) {
            String authority = "DEPT_" + udr.getDepartment().getName() + ":" + udr.getRoleInDept().name();
            authorities.add(new SimpleGrantedAuthority(authority));
        }

        return authorities;
    }
}

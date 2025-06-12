package staport.finance.gBanque.security.services;


import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import staport.finance.gBanque.security.entities.User;
import staport.finance.gBanque.security.repositories.UserRepository;

import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User appUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new org.springframework.security.core.userdetails.User(
                appUser.getUsername(),
                appUser.getPassword(),
                appUser.isEnabled(),
                true, true, true,
                appUser.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getRoleName().name()))
                        .collect(Collectors.toList())
        );
    }
}

package staport.finance.gBanque.security.services;

import org.springframework.stereotype.Service;
import staport.finance.gBanque.security.repositories.UserRepository;

import java.util.List;

@Service
public class UserService {


    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public List<String> getAllDistinctUsernames() {
        return userRepository.findDistinctUsernames();
    }
}

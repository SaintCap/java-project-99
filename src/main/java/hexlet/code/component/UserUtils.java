package hexlet.code.component;

import hexlet.code.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("userUtils")
@RequiredArgsConstructor
public class UserUtils {

    private final UserRepository userRepository;

    public boolean isCurrentUser(long id) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return userRepository.findById(id)
                .map(user -> user.getEmail().equals(authentication.getName()))
                .orElse(false);
    }
}

package mate.academy.accommodationbookingservice.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import mate.academy.accommodationbookingservice.model.User;

public interface AuthenticatedUserProvider {
    default User getUserFromAuth(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (user == null) {
            throw new UsernameNotFoundException(
                    "Can't find user with such email: "
                            + authentication.getName()
            );
        }
        return user;
    }
}

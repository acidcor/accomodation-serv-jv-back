package mate.academy.accommodationbooking.service.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import mate.academy.accommodationbooking.service.model.User;

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

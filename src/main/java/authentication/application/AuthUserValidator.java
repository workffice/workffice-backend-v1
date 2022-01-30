package authentication.application;

import io.vavr.control.Option;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AuthUserValidator {
    public boolean isSameUserAsAuthenticated(String email) {
        Option<Authentication> authentication = Option.of(
                SecurityContextHolder.getContext().getAuthentication()
        );
        return authentication
                .map(auth -> (UserDetails) auth.getPrincipal())
                .filter(user -> user.getUsername().equals(email))
                .isDefined();
    }
}

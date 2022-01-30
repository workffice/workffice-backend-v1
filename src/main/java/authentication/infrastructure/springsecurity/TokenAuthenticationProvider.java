package authentication.infrastructure.springsecurity;

import authentication.application.AuthUserFinder;
import authentication.domain.token.Token;

import java.util.ArrayList;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class TokenAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {
    private final AuthUserFinder authUserFinder;

    public TokenAuthenticationProvider(AuthUserFinder authUserFinder) {
        this.authUserFinder = authUserFinder;
    }

    @Override
    protected void additionalAuthenticationChecks(
            UserDetails userDetails,
            UsernamePasswordAuthenticationToken authentication
    ) throws AuthenticationException {

    }

    @Override
    protected UserDetails retrieveUser(
            String username,
            UsernamePasswordAuthenticationToken authentication
    ) throws AuthenticationException {
        String token = (String) authentication.getPrincipal();
        return authUserFinder
                .find(new Token(token))
                .map(authUser -> new User(authUser.email(), authUser.password(), new ArrayList<>()))
                .getOrElseThrow(() -> new UsernameNotFoundException("There is no user with email provided"));
    }
}

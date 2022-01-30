package authentication.infrastructure;

import authentication.application.AuthUserFinder;
import authentication.domain.token.Token;
import authentication.domain.user.AuthUser;
import authentication.factories.AuthUserBuilder;
import authentication.infrastructure.springsecurity.TokenAuthenticationProvider;
import io.vavr.control.Option;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestTokenAuthenticationProvider {
    
    AuthUserFinder finder = mock(AuthUserFinder.class);
    TokenAuthenticationProvider authenticationProvider = new TokenAuthenticationProvider(finder);
    
    @Test
    public void itShouldRaiseBadCredentialsWhenFinderReturnsEmptyAuthUser() {
        when(finder.find(any(Token.class))).thenReturn(Option.none());
        
        assertThatThrownBy(() -> authenticationProvider
                .authenticate(new UsernamePasswordAuthenticationToken(
                        "principal", "credentials"
                ))
        ).isInstanceOf(BadCredentialsException.class)
                .hasMessage("Bad credentials");
        
    }
    
    @Test
    public void itShouldReturnUserWithEmailAndPasswordFromAuthUserReturnedByFinder() {
        AuthUser authUser = new AuthUserBuilder().build();
        when(finder.find(any(Token.class))).thenReturn(Option.of(authUser));
        
        Authentication authentication = authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken("provider", "credentials")
        );
        
        User user = (User) authentication.getPrincipal();
        assertThat(user.getUsername()).isEqualTo(authUser.email());
        assertThat(user.getPassword()).isEqualTo(authUser.password());
        assertThat(user.getAuthorities()).isEmpty();
    }
}

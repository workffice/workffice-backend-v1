package authentication.infrastructure;

import authentication.application.TokenBlockListFinder;
import authentication.domain.token.Token;
import authentication.domain.token.TokenRepository;
import authentication.infrastructure.springsecurity.TokenAuthenticationFilter;
import io.vavr.control.Option;

import java.io.IOException;
import javax.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestTokenAuthenticationFilter {
    
    AuthenticationManager authManager = mock(AuthenticationManager.class);
    RequestMatcher requireAuthentication = new AntPathRequestMatcher("/");
    TokenRepository tokenRepository = mock(TokenRepository.class);
    TokenBlockListFinder tokenBlockListFinder = new TokenBlockListFinder(tokenRepository);
    
    @Test
    public void itShouldRaiseBadCredentialExceptionWhenTokenIsNotProvided() {
        TokenAuthenticationFilter filter = new TokenAuthenticationFilter(
                requireAuthentication,
                tokenBlockListFinder
        );
        filter.setAuthenticationManager(authManager);
        
        assertThatThrownBy(() -> {
            filter.attemptAuthentication(new MockHttpServletRequest(), new MockHttpServletResponse());
        }).isInstanceOf(BadCredentialsException.class).hasMessage("No token provided");
    }
    
    @Test
    public void itShouldCallAuthenticationManagerWhenTokenIsProvided() throws IOException, ServletException {
        TokenAuthenticationFilter filter = new TokenAuthenticationFilter(
                requireAuthentication,
                tokenBlockListFinder
        );
        filter.setAuthenticationManager(authManager);
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        when(tokenRepository.find("super_special_token")).thenReturn(Option.none());
        mockRequest.addHeader("Authorization", "Bearer super_special_token");
        
        filter.attemptAuthentication(mockRequest, new MockHttpServletResponse());
        
        verify(authManager, times(1))
                .authenticate(new UsernamePasswordAuthenticationToken(
                        "super_special_token", "super_special_token"
                ));
    }
    
    @Test
    public void itShouldRaiseBadCredentialExceptionWhenTokenIsInBlockedList() {
        TokenAuthenticationFilter filter = new TokenAuthenticationFilter(
                requireAuthentication,
                tokenBlockListFinder
        );
        filter.setAuthenticationManager(authManager);
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader("Authorization", "Bearer super_special_token");
        when(tokenRepository.find("super_special_token")).thenReturn(Option.of(new Token("super_special_token")));
        
        assertThatThrownBy(() -> filter.attemptAuthentication(mockRequest, new MockHttpServletResponse()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Token is invalid");
    }
}

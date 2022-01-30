package authentication.infrastructure.springsecurity;

import authentication.application.TokenBlockListFinder;

import java.io.IOException;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class TokenAuthenticationFilter extends AbstractAuthenticationProcessingFilter {
    
    private final TokenBlockListFinder tokenBlockListFinder;

    public TokenAuthenticationFilter(
            RequestMatcher requireAuthentication,
            TokenBlockListFinder tokenBlockListFinder
    ) {
        super(requireAuthentication);
        this.tokenBlockListFinder = tokenBlockListFinder;
    }

    @Override
    public Authentication attemptAuthentication(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws AuthenticationException, IOException, ServletException {
        String authenticationHeader = request.getHeader("Authorization");
        String token = Optional.ofNullable(authenticationHeader)
                .map(s -> s.replace("Bearer ", ""))
                .orElseThrow(() -> new BadCredentialsException("No token provided"));
        if (tokenBlockListFinder.findToken(token).isDefined())
            throw new BadCredentialsException("Token is invalid");
        Authentication authentication = new UsernamePasswordAuthenticationToken(token, token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return getAuthenticationManager().authenticate(authentication);
    }

    protected void successfulAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain,
            Authentication authResult
    ) throws IOException, ServletException {
        super.successfulAuthentication(request, response, chain, authResult);
        chain.doFilter(request, response);
    }
}

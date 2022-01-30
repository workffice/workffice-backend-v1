package authentication.application;

import authentication.application.dto.user.AuthUserResponse;
import authentication.domain.token.Token;
import authentication.domain.token.TokenGenerator;
import authentication.domain.user.AuthUser;
import authentication.domain.user.AuthUserRepository;
import backoffice.application.UserTypeResolver;
import io.vavr.control.Option;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AuthUserFinder {

    private final AuthUserRepository authUserRepo;
    private final TokenGenerator     tokenGenerator;
    private final UserTypeResolver   userTypeResolver;

    public AuthUserFinder(
            AuthUserRepository authUserRepo,
            TokenGenerator     tokenGenerator,
            UserTypeResolver   userTypeResolver
    ) {
        this.authUserRepo     = authUserRepo;
        this.tokenGenerator   = tokenGenerator;
        this.userTypeResolver = userTypeResolver;
    }

    public Option<AuthUser> find(Token token) {
        return tokenGenerator
                .parseToken(token)
                .flatMap(authUserRepo::findById);
    }

    public Option<AuthUserResponse> findAuthenticatedUser() {
        Option<Authentication> authentication = Option.of(
                SecurityContextHolder.getContext().getAuthentication()
        );
        return authentication
                .map(auth -> (UserDetails) auth.getPrincipal())
                .flatMap(userDetails -> authUserRepo.findByEmail(userDetails.getUsername()))
                .map(authUser -> {
                    UserTypeResolver.UserType userType = userTypeResolver.getUserType(authUser.email());
                    return authUser.toResponse(userType.name());
                });
    }
}

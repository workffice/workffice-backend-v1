package authentication.application;

import authentication.application.dto.token.Authentication;
import authentication.application.dto.user.UserError;
import authentication.application.dto.user.UserLoginInformation;
import authentication.domain.token.Token;
import authentication.domain.token.TokenGenerator;
import authentication.domain.token.TokenRepository;
import authentication.domain.user.AuthUser;
import authentication.domain.user.AuthUserRepository;
import authentication.domain.user.PasswordEncoder;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import org.springframework.stereotype.Service;

@Service
public class AuthUserAuthenticator {

    private final AuthUserRepository userRepo;
    private final TokenRepository tokenRepo;
    private final PasswordEncoder encoder;
    private final TokenGenerator tokenGenerator;

    public AuthUserAuthenticator(
            AuthUserRepository userRepo,
            TokenRepository tokenRepo,
            PasswordEncoder encoder,
            TokenGenerator tokenGenerator
    ) {
        this.tokenRepo = tokenRepo;
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.tokenGenerator = tokenGenerator;
    }

    public Either<UseCaseError, Authentication> login(UserLoginInformation information) {
        return userRepo.findByEmail(information.getEmail())
                .toEither((UseCaseError) UserError.USER_NOT_FOUND)
                .filterOrElse(
                        AuthUser::isActive,
                        authUser -> UserError.NON_ACTIVE_USER
                )
                .filterOrElse(
                        authUser -> encoder.match(information.getPassword(), authUser.password()),
                        authUser -> UserError.INVALID_PASSWORD
                ).map(authUser -> new Authentication(tokenGenerator.create(authUser).token()));
    }

    public void logout(Token token) {
        tokenRepo.store(token);
    }
}

package authentication.application;

import authentication.application.dto.token.TokenError;
import authentication.application.dto.user.UserError;
import authentication.domain.token.Token;
import authentication.domain.token.TokenGenerator;
import authentication.domain.token.TokenRepository;
import authentication.domain.user.AuthUser;
import authentication.domain.user.AuthUserRepository;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import org.springframework.stereotype.Service;

@Service
public class AuthUserActivator {

    private final TokenRepository tokenRepository;
    private final AuthUserRepository authUserRepository;
    private final TokenGenerator tokenGenerator;

    public AuthUserActivator(
            TokenRepository tokenRepository,
            AuthUserRepository authUserRepository,
            TokenGenerator tokenGenerator
    ) {
        this.tokenRepository = tokenRepository;
        this.authUserRepository = authUserRepository;
        this.tokenGenerator = tokenGenerator;
    }

    public Either<UseCaseError, AuthUser> activateUser(Token token) {
        if (tokenRepository.find(token.token()).isDefined())
            return Either.left(TokenError.TOKEN_ALREADY_USED);
        return tokenGenerator.parseToken(token)
                .toEither((UseCaseError) TokenError.INVALID_TOKEN)
                .flatMap(userId -> authUserRepository
                        .findById(userId)
                        .toEither(UserError.USER_NOT_FOUND))
                .peek(user -> {
                    user.activate();
                    authUserRepository.update(user);
                    tokenRepository.store(token);
                });
    }
}

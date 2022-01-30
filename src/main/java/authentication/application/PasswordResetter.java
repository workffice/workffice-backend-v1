package authentication.application;

import authentication.application.dto.token.TokenError;
import authentication.application.dto.user.PasswordResetInformation;
import authentication.application.dto.user.UserError;
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
public class PasswordResetter {

    private final AuthUserRepository authUserRepo;
    private final TokenRepository tokenRepo;
    private final TokenGenerator tokenGenerator;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetter(
            AuthUserRepository authUserRepo,
            TokenRepository tokenRepo,
            TokenGenerator tokenGenerator,
            PasswordEncoder passwordEncoder
    ) {
        this.authUserRepo = authUserRepo;
        this.tokenRepo = tokenRepo;
        this.tokenGenerator = tokenGenerator;
        this.passwordEncoder = passwordEncoder;
    }

    private Either<UseCaseError, Void> updatePassword(AuthUser authUser, String newPassword) {
        authUser.updatePassword(passwordEncoder.encode(newPassword));
        return authUserRepo.update(authUser).toEither(UserError.DB_ERROR);
    }

    public Either<UseCaseError, Void> updatePassword(Token token, PasswordResetInformation passwordResetInformation) {
        if (tokenRepo.find(token.token()).isDefined())
            return Either.left(TokenError.TOKEN_ALREADY_USED);
        return tokenGenerator
                .parseToken(token)
                .toEither((UseCaseError) TokenError.INVALID_TOKEN)
                .flatMap(authUserId -> authUserRepo.findById(authUserId).toEither(UserError.USER_NOT_FOUND))
                .flatMap(authUser -> updatePassword(authUser, passwordResetInformation.getPassword()))
                .peek(v -> tokenRepo.store(token));
    }
}

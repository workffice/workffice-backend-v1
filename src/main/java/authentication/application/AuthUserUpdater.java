package authentication.application;

import authentication.application.dto.user.UserError;
import authentication.application.dto.user.UserUpdateInformation;
import authentication.domain.user.AuthUserId;
import authentication.domain.user.AuthUserRepository;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import org.springframework.stereotype.Service;

@Service
public class AuthUserUpdater {
    private final AuthUserRepository authUserRepo;
    private final AuthUserValidator  authUserValidator;

    public AuthUserUpdater(AuthUserRepository authUserRepo, AuthUserValidator authUserValidator) {
        this.authUserRepo      = authUserRepo;
        this.authUserValidator = authUserValidator;
    }

    public Either<UseCaseError, Void> update(AuthUserId id, UserUpdateInformation info) {
        return authUserRepo
                .findById(id)
                .toEither((UseCaseError) UserError.USER_NOT_FOUND)
                .filterOrElse(
                        authUser -> authUserValidator.isSameUserAsAuthenticated(authUser.email()),
                        a -> UserError.FORBIDDEN
                )
                .map(authUser -> authUser.update(
                        info.getName(),
                        info.getLastname(),
                        info.getAddress(),
                        info.getBio(),
                        info.getProfileImage()
                ))
                .flatMap(authUser -> authUserRepo.update(authUser).toEither(UserError.DB_ERROR));
    }
}

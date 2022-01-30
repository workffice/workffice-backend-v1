package controller;

import authentication.application.AuthUserCreator;
import authentication.application.AuthUserFinder;
import authentication.application.AuthUserUpdater;
import authentication.application.PasswordResetRequester;
import authentication.application.dto.user.UserEmailRequest;
import authentication.application.dto.user.UserInformation;
import authentication.application.dto.user.UserUpdateInformation;
import authentication.domain.user.AuthUserId;
import controller.response.DataResponse;
import controller.response.SingleResponse;

import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static authentication.application.dto.user.UserError.FORBIDDEN;
import static authentication.application.dto.user.UserError.USER_EMAIL_ALREADY_EXISTS;
import static authentication.application.dto.user.UserError.USER_NOT_FOUND;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static java.lang.String.format;


@RestController
@RequestMapping(value = "/api/users")
@Validated
public class AuthUserController extends BaseController {
    private final ResponseEntity<DataResponse> notFound = ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(notFound("USER_NOT_FOUND", "There is no user with email requested"));
    private final ResponseEntity<DataResponse> forbidden = ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(forbidden("USER_FORBIDDEN", "You don't have access to this auth user"));

    @Autowired AuthUserCreator        creator;
    @Autowired AuthUserFinder         finder;
    @Autowired AuthUserUpdater        updater;
    @Autowired PasswordResetRequester passwordResetRequester;

    @PostMapping(value = "/")
    public ResponseEntity<?> create(@Valid @RequestBody UserInformation information) {
        AuthUserId id = new AuthUserId();
        DataResponse createdResponse = entityCreated(format("/api/users/%s/", id));
        DataResponse failureResponse = invalid("USER_ALREADY_EXISTS", "There is another user with the same email");

        return creator.createUser(id, information)
                .map(u -> new ResponseEntity<>(createdResponse, HttpStatus.CREATED))
                .getOrElseGet(error -> Match(error).of(
                        Case($(USER_EMAIL_ALREADY_EXISTS), ResponseEntity.badRequest().body(failureResponse))
                ));
    }

    @PutMapping(value = "/{id}/")
    public ResponseEntity<?> updateUser(
            @PathVariable String id,
            @RequestBody UserUpdateInformation info
    ) {
        try {
            var authUserId = AuthUserId.fromString(id);
            return updater.update(authUserId, info)
                    .fold(
                            error -> Match(error).of(
                                    Case($(USER_NOT_FOUND), notFound),
                                    Case($(FORBIDDEN), forbidden)
                            ),
                            v -> ResponseEntity.ok(entityResponse("success :'("))
                    );
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(invalid("INVALID_USER_ID", "The user id provided is invalid"));
        }
    }

    @GetMapping(value = "/me/")
    public ResponseEntity<?> me() {
        return finder
                .findAuthenticatedUser()
                .map(authUser -> ResponseEntity.ok(new SingleResponse<>(authUser)))
                .get();
    }

    @PostMapping(value = "/password_reset_requests/")
    public ResponseEntity<?> requestPasswordReset(
            @Valid @RequestBody UserEmailRequest userEmail
    ) {
        return passwordResetRequester
                .requestPasswordReset(userEmail.getUserEmail())
                // Body needs to be removed once the FE is decent
                .map(v -> ResponseEntity.accepted().body((DataResponse) entityResponse("success :'(")))
                .getOrElseGet(error -> Match(error).of(Case($(USER_NOT_FOUND), notFound)));
    }
}

package controller;

import authentication.application.AuthUserActivator;
import authentication.application.PasswordResetter;
import authentication.application.dto.token.TokenError;
import authentication.application.dto.user.PasswordResetInformation;
import authentication.application.dto.user.UserError;
import authentication.domain.token.Token;
import backoffice.application.collaborator.CollaboratorActivator;
import backoffice.application.dto.collaborator.CollaboratorError;
import backoffice.domain.collaborator.CollaboratorToken;
import controller.response.DataResponse;
import controller.response.ErrorResponse;

import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

@RestController
@RequestMapping(value = "/api/confirmation_tokens")
public class ActivationController extends BaseController {

    ErrorResponse tokenAlreadyUsed     = invalid("TOKEN_ALREADY_USED", "Token provided was already used");
    ErrorResponse invalidToken         = invalid("INVALID_TOKEN", "Token provided is invalid or maybe has expired");
    ErrorResponse userNotFound         = invalid("USER_NOT_FOUND", "There is no user associated with token");
    ErrorResponse collaboratorNotFound = invalid(
            "COLLABORATOR_NOT_FOUND",
            "There is no collaborator associated with token"
    );

    @Autowired private AuthUserActivator     authUserActivator;
    @Autowired private PasswordResetter      passwordResetter;
    @Autowired private CollaboratorActivator collaboratorActivator;

    @PostMapping(value = "/account_activations/{token}/")
    public ResponseEntity<?> activateUser(@PathVariable String token) {
        return authUserActivator.activateUser(new Token(token))
                .map(user -> ResponseEntity.ok((DataResponse) entityResponse("success :'(")))
                .getOrElseGet(error -> Match(error).of(
                        Case($(TokenError.TOKEN_ALREADY_USED), ResponseEntity.badRequest().body(tokenAlreadyUsed)),
                        Case($(TokenError.INVALID_TOKEN), ResponseEntity.badRequest().body(invalidToken)),
                        Case($(UserError.USER_NOT_FOUND), ResponseEntity.badRequest().body(userNotFound))
                ));
    }

    @PostMapping(value = "/password_resets/{token}/")
    public ResponseEntity<?> resetPassword(
            @PathVariable String token,
            @RequestBody @Valid PasswordResetInformation info
    ) {
        return passwordResetter
                .updatePassword(new Token(token), info)
                .map(v -> ResponseEntity.accepted().body((DataResponse) entityResponse("success :'(")))
                .getOrElseGet(error -> Match(error).of(
                        Case($(TokenError.TOKEN_ALREADY_USED), ResponseEntity.badRequest().body(tokenAlreadyUsed)),
                        Case($(TokenError.INVALID_TOKEN), ResponseEntity.badRequest().body(invalidToken)),
                        Case($(UserError.USER_NOT_FOUND), ResponseEntity.badRequest().body(userNotFound))
                ));
    }

    @PostMapping(value = "/collaborator_activations/{token}/")
    public ResponseEntity<?> activateCollaborator(@PathVariable String token) {
        return collaboratorActivator
                .activate(CollaboratorToken.of(token))
                .map(v -> ResponseEntity.accepted().body((DataResponse) entityResponse("success :'(")))
                .getOrElseGet(error -> Match(error).of(
                        Case($(CollaboratorError.INVALID_TOKEN), ResponseEntity.badRequest().body(invalidToken)),
                        Case($(CollaboratorError.COLLABORATOR_NOT_FOUND), ResponseEntity
                                .badRequest().body(collaboratorNotFound))
                ));
    }
}

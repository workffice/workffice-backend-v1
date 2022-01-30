package controller;

import authentication.application.AuthUserAuthenticator;
import authentication.application.dto.user.UserLoginInformation;
import authentication.domain.token.Token;
import controller.response.DataResponse;
import controller.response.ErrorResponse;
import controller.response.SingleResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static authentication.application.dto.user.UserError.INVALID_PASSWORD;
import static authentication.application.dto.user.UserError.NON_ACTIVE_USER;
import static authentication.application.dto.user.UserError.USER_NOT_FOUND;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

@RestController
@RequestMapping(value = "/api/authentications")
public class AuthenticationController extends BaseController {

    @Autowired
    AuthUserAuthenticator authenticator;

    @DeleteMapping("/{token}/")
    public ResponseEntity<?> logout(@PathVariable String token) {
        authenticator.logout(new Token(token));
        return ResponseEntity.ok(entityResponse("success :`("));
    }

    @PostMapping("/")
    public ResponseEntity<?> login(@RequestBody UserLoginInformation information) {
        ErrorResponse userNotFound = notFound("USER_NOT_FOUND", "There is no user with email provided");
        ErrorResponse invalidPassword = invalid("INVALID_PASSWORD", "Password is incorrect");
        ErrorResponse nonActiveUser = invalid("NON_ACTIVE_USER", "The user is not active in the system");
        return authenticator
                .login(information)
                .map(authentication -> ResponseEntity.ok((DataResponse) new SingleResponse<>(authentication)))
                .getOrElseGet(error -> Match(error).of(
                        Case($(USER_NOT_FOUND), ResponseEntity.status(HttpStatus.NOT_FOUND).body(userNotFound)),
                        Case($(INVALID_PASSWORD), ResponseEntity.badRequest().body(invalidPassword)),
                        Case($(NON_ACTIVE_USER), ResponseEntity.badRequest().body(nonActiveUser))
                ));
    }
}

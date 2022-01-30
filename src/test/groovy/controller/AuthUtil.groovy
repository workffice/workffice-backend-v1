package controller

import authentication.application.AuthUserAuthenticator
import authentication.application.dto.token.Authentication
import authentication.application.dto.user.UserLoginInformation
import authentication.domain.user.AuthUser
import authentication.domain.user.AuthUserRepository
import authentication.domain.user.PasswordEncoder
import authentication.domain.user.Status
import authentication.factories.AuthUserBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AuthUtil {
    @Autowired
    private AuthUserRepository authUserRepo
    @Autowired
    private AuthUserAuthenticator authenticator
    @Autowired
    private PasswordEncoder passwordEncoder

    Authentication createAndLoginUser(String email, String password) {
        AuthUser authUser = new AuthUserBuilder()
                .withEmail(email)
                .withPassword(passwordEncoder.encode(password))
                .withStatus(Status.ACTIVE)
                .build()
        authUserRepo.store(authUser)
        return authenticator
                .login(new UserLoginInformation(authUser.email(), password))
                .get()
    }

    Authentication loginUser(String email, String password) {
        return authenticator
                .login(new UserLoginInformation(email, password))
                .get()
    }
}

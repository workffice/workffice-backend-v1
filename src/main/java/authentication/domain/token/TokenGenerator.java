package authentication.domain.token;

import authentication.domain.user.AuthUser;
import authentication.domain.user.AuthUserId;
import io.vavr.control.Option;

public interface TokenGenerator {

    Token create(AuthUser authUser);

    Option<AuthUserId> parseToken(Token token);
}

package authentication.domain.user;

import io.vavr.control.Option;
import io.vavr.control.Try;

public interface AuthUserRepository {

    Try<Void> store(AuthUser authUser);
    
    Try<Void> update(AuthUser authUser);

    Option<AuthUser> findByEmail(String username);

    Option<AuthUser> findById(AuthUserId id);
}

package authentication.infrastructure.repositories;

import authentication.domain.user.AuthUser;
import authentication.domain.user.AuthUserId;
import authentication.domain.user.AuthUserRepository;
import authentication.domain.user.UserEmailAlreadyExist;
import io.vavr.control.Option;
import io.vavr.control.Try;

import javax.persistence.PersistenceException;
import org.springframework.stereotype.Repository;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.Predicates.instanceOf;

@Repository
public class AuthUserMysqlRepo extends AuthenticationJPARepo<AuthUser, AuthUserId> implements AuthUserRepository {
    public Try<Void> update(AuthUser authUser) {
        return this.merge(authUser);
    }
    
    @Override
    public Try<Void> store(AuthUser authUser) {
        //noinspection unchecked
        return this.save(authUser)
                .mapFailure(Case($(instanceOf(PersistenceException.class)), UserEmailAlreadyExist::new));
    }
    
    @Override
    public Option<AuthUser> findByEmail(String email) {
        return this.findByColumn("email", email);
    }
    
    @Override
    public Option<AuthUser> findById(AuthUserId id) {
        return super.findById(id);
    }
    
    @Override
    public Class<AuthUser> getEntityClass() {
        return AuthUser.class;
    }
}

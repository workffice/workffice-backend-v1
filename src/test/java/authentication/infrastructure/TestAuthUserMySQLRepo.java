package authentication.infrastructure;

import authentication.domain.user.AuthUser;
import authentication.domain.user.AuthUserId;
import authentication.domain.user.Status;
import authentication.domain.user.UserEmailAlreadyExist;
import authentication.factories.AuthUserBuilder;
import authentication.infrastructure.repositories.AuthUserMysqlRepo;
import io.vavr.control.Option;
import server.WorkfficeApplication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ContextConfiguration(classes = {WorkfficeApplication.class})
public class TestAuthUserMySQLRepo {
    
    @Autowired
    AuthUserMysqlRepo repo;
    
    @Test
    public void itShouldStoreAuthUserIntoDatabase() {
        AuthUser user = new AuthUserBuilder().build();
        
        repo.store(user);
        
        AuthUser userStored = repo.findByEmail(user.email()).get();
        assertThat(userStored.email()).isEqualTo(user.email());
        assertThat(userStored.password()).isEqualTo(user.password());
    }
    
    @Test
    void itShouldUpdateAuthUserInformation() {
        AuthUser user = new AuthUserBuilder()
                .withStatus(Status.PENDING)
                .build();
        repo.store(user);
        
        user.activate();
        repo.update(user);
        
        AuthUser authUserUpdated = repo.findById(user.id()).get();
        assertThat(authUserUpdated.isActive()).isTrue();
    }
    
    @Test
    public void itShouldReturnEmptyUserWhenThereIsNoUserWithEmailSpecified() {
        Option<AuthUser> userStored = repo.findByEmail("unexistent@mail.com");
        
        assertThat(userStored.isEmpty()).isTrue();
    }
    
    @Test
    public void itShouldReturnUserWithIdSpecified() {
        AuthUserId id = new AuthUserId();
        AuthUser user = new AuthUserBuilder().withId(id).build();
        
        repo.store(user);
        
        AuthUser userStored = repo.findById(id).get();
        assertThat(userStored.email()).isEqualTo(user.email());
        assertThat(userStored.password()).isEqualTo(user.password());
    }
    
    @Test
    public void itShouldReturnEmptyUserWhenThereIsNoUserWithIdSpecified() {
        Option<AuthUser> userStored = repo.findById(new AuthUserId());
        
        assertThat(userStored.isEmpty()).isTrue();
    }
    
    @Test
    public void itShouldReturnUserEmailAlreadyExistWhenThereIsAUserWithSameEmail() {
        AuthUser user = new AuthUserBuilder().withEmail("test@mail.com").build();
        AuthUser user2 = new AuthUserBuilder().withEmail("test@mail.com").build();
        
        repo.store(user);
        
        assertThatThrownBy(() -> repo.store(user2).get()).isInstanceOf(UserEmailAlreadyExist.class);
    }
}

package authentication.factories;

import authentication.domain.user.AuthUser;
import authentication.domain.user.AuthUserId;
import authentication.domain.user.Status;
import com.github.javafaker.Faker;

public class AuthUserBuilder {
    private final Faker faker = new Faker();
    private AuthUserId id = new AuthUserId();
    private String email = faker.internet().emailAddress();
    private String password = faker.internet().password();
    private Status status = Status.ACTIVE;
    private String name = faker.name().name();
    private String lastname = faker.name().lastName();
    private String address = faker.address().fullAddress();
    private String bio = faker.lorem().characters();
    private String profileImage = faker.internet().image();
    
    public AuthUserBuilder withId(AuthUserId id) {
        this.id = id;
        return this;
    }
    
    public AuthUserBuilder withEmail(String email) {
        this.email = email;
        return this;
    }
    
    public AuthUserBuilder withPassword(String password) {
        this.password = password;
        return this;
    }
    
    public AuthUserBuilder withStatus(Status status) {
        this.status = status;
        return this;
    }
    
    public AuthUser build() {
        return new AuthUser(
                id,
                email,
                password,
                status,
                name,
                lastname,
                address,
                bio,
                profileImage
        );
    }
}

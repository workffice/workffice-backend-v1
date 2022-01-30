package authentication.domain.user;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import shared.domain.DomainEvent;

@Getter
@EqualsAndHashCode(of = {"id", "email", "userType"}, callSuper = false)
public class UserCreatedEvent extends DomainEvent {
    private final String id;
    private final String email;
    private final String userType;

    public UserCreatedEvent(String id, String email, String userType) {
        this.id = id;
        this.email = email;
        this.userType = userType;
    }

    @Override
    public String getEventName() {
        return "USER_CREATED";
    }
}

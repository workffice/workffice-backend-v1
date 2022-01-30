package authentication.domain.user;

import shared.domain.DomainId;

import java.util.UUID;

public class AuthUserId extends DomainId {
    public AuthUserId() {
        super();
    }

    public AuthUserId(UUID id) {
        super(id);
    }

    public static AuthUserId fromString(String id) {
        return new AuthUserId(UUID.fromString(id));
    }
}

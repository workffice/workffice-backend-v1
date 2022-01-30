package backoffice.domain.role;

import shared.domain.DomainId;

import java.util.UUID;

public class RoleId extends DomainId {
    public RoleId() {
        super();
    }

    public RoleId(UUID id) {
        super(id);
    }

    public static RoleId fromString(String id) {
        return new RoleId(UUID.fromString(id));
    }
}

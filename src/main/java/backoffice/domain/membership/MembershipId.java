package backoffice.domain.membership;

import shared.domain.DomainId;

import java.util.UUID;

public class MembershipId extends DomainId {
    public MembershipId() {
        super();
    }

    public MembershipId(UUID id) {
        super(id);
    }

    public static MembershipId fromString(String id) {
        return new MembershipId(UUID.fromString(id));
    }
}

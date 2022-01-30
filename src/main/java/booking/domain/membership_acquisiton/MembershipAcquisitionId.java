package booking.domain.membership_acquisiton;

import shared.domain.DomainId;

import java.util.UUID;

public class MembershipAcquisitionId extends DomainId {
    public MembershipAcquisitionId() {
        super(UUID.randomUUID());
    }

    public MembershipAcquisitionId(UUID id) {
        super(id);
    }

    public static MembershipAcquisitionId fromString(String id) {
        return new MembershipAcquisitionId(UUID.fromString(id));
    }
}

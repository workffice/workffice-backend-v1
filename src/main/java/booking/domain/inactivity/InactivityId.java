package booking.domain.inactivity;

import shared.domain.DomainId;

import java.util.UUID;

public class InactivityId extends DomainId {
    public InactivityId() {
        super();
    }

    public InactivityId(UUID id) {
        super(id);
    }

    public static InactivityId fromString(String id) {
        return new InactivityId(UUID.fromString(id));
    }
}

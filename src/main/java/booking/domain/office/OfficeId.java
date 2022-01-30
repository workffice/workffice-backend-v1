package booking.domain.office;

import shared.domain.DomainId;

import java.util.UUID;

public class OfficeId extends DomainId {
    public OfficeId() {
        super();
    }

    public OfficeId(UUID id) {
        super(id);
    }

    public static OfficeId fromString(String id) {
        return new OfficeId(UUID.fromString(id));
    }
}

package booking.domain.booking;

import shared.domain.DomainId;

import java.util.UUID;

public class BookingId extends DomainId {
    public BookingId() {
        super(UUID.randomUUID());
    }

    public BookingId(UUID id) {
        super(id);
    }

    public static BookingId fromString(String id) { return new BookingId(UUID.fromString(id)); }
}

package booking.domain.office;

import booking.application.dto.booking.BookingError;
import booking.domain.booking.Booking;
import booking.domain.booking.BookingId;
import booking.domain.inactivity.Inactivity;
import booking.domain.office.privacy.Privacy;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.NoArgsConstructor;
import shared.application.UseCaseError;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Table(name = "offices")
@Entity
@NoArgsConstructor
public class Office {
    @EmbeddedId
    private OfficeId id;
    @Column
    private String officeBranchId;
    @Column
    private String name;
    @Column
    private Integer price;
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private Privacy privacy;
    @OneToMany(cascade = {CascadeType.MERGE})
    @JoinColumn(name = "officeId")
    private List<Inactivity> inactivities;
    @Column
    private boolean deleted;

    private Office(
            OfficeId         id,
            String           officeBranchId,
            String           name,
            Integer          price,
            Privacy          privacy,
            List<Inactivity> inactivities
    ) {
        this.id             = id;
        this.officeBranchId = officeBranchId;
        this.name           = name;
        this.price          = price;
        this.privacy        = privacy;
        this.inactivities   = inactivities;
        this.deleted        = false;
    }

    public static Office create(OfficeId id, String officeBranchId, String name, Integer price, Privacy privacy) {
        return new Office(id, officeBranchId, name, price, privacy, new ArrayList<>());
    }

    private static LocalDateTime toUTC(ZonedDateTime dateTime) {
        return dateTime.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
    }

    public OfficeId id() { return id; }

    public String name() { return name; }

    public Integer price() { return price; }

    public String officeBranchId() { return officeBranchId; }

    public Office update(String name, Integer price, Privacy privacy) {
        this.name = name;
        this.price = price;
        this.privacy = privacy;
        return this;
    }

    public Office delete() {
        this.deleted = true;
        return this;
    }

    public void addInactivity(Inactivity inactivity) {
        this.inactivities.add(inactivity);
    }

    public List<Inactivity> inactivities() { return inactivities; }

    public boolean isDeleted() { return deleted; }

    public Either<UseCaseError, Booking> book(
            String renterEmail,
            Integer attendeesQuantity,
            ZonedDateTime proposedStartTime,
            ZonedDateTime proposedEndTime,
            List<Booking> existentBookings
    ) {
        Try<Booking> bookingOrError = Booking.create(
                new BookingId(),
                this,
                proposedStartTime,
                proposedEndTime,
                renterEmail,
                attendeesQuantity
        );
        var proposedStartTimeUtc = toUTC(proposedStartTime);
        var proposedEndTimeUtc = toUTC(proposedEndTime);
        Predicate<Booking> officeSupportsBooking = booking -> {
            var isAvailable = inactivities.stream()
                    .noneMatch(inactivity -> inactivity.isUnavailableAt(proposedStartTimeUtc.toLocalDate())
                            || inactivity.isUnavailableAt(proposedEndTimeUtc.toLocalDate()));
            var hasNoConflictsWithOtherBookings = privacy.canBeBooked(
                    proposedStartTimeUtc,
                    proposedEndTimeUtc,
                    existentBookings
            );
            return isAvailable && hasNoConflictsWithOtherBookings;
        };
        return bookingOrError
                .toEither((UseCaseError) BookingError.INVALID_SCHEDULE_TIME)
                .filterOrElse(
                    officeSupportsBooking,
                    b -> BookingError.OFFICE_IS_NOT_AVAILABLE
        );
    }
}

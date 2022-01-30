package booking.application.booking.creation;

import booking.application.dto.booking.BookingInformation;
import booking.application.dto.membership_acquisition.MembershipAcquisitionError;
import booking.domain.booking.Booking;
import booking.domain.booking.BookingRepository;
import booking.domain.booking.PaymentInformation;
import booking.domain.membership_acquisiton.MembershipAcquisitionId;
import booking.domain.membership_acquisiton.MembershipAcquisitionRepository;
import booking.domain.office.Office;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class BookingFromMembershipCreator implements BookingCreationStrategy {
    private static final ZoneId timezoneARG = ZoneId.of("America/Argentina/Buenos_Aires");
    private final BookingRepository               bookingRepo;
    private final MembershipAcquisitionRepository membershipAcquisitionRepo;
    private final MembershipAcquisitionId         membershipAcquisitionId;

    public BookingFromMembershipCreator(
            BookingRepository               bookingRepo,
            MembershipAcquisitionRepository membershipAcquisitionRepo,
            MembershipAcquisitionId         membershipAcquisitionId
    ) {
        this.bookingRepo               = bookingRepo;
        this.membershipAcquisitionRepo = membershipAcquisitionRepo;
        this.membershipAcquisitionId   = membershipAcquisitionId;
    }

    private List<Booking> findExistentBookings(Office office, LocalDate scheduleDate) {
        return bookingRepo.find(office, scheduleDate)
                .stream().filter(Booking::isActive)
                .collect(Collectors.toList());
    }

    private Either<UseCaseError, Booking> createBooking(Office office, String renterEmail, BookingInformation info) {
        var argStartTime = ZonedDateTime.of(info.getStartTime(), timezoneARG);
        var existentBookings = findExistentBookings(
                office,
                argStartTime.withZoneSameInstant(ZoneId.of("UTC")).toLocalDate()
        );
        return office.book(
                renterEmail,
                info.getAttendeesQuantity(),
                argStartTime,
                ZonedDateTime.of(info.getEndTime(), timezoneARG),
                existentBookings)
                .map(booking -> {
                    var paymentInformation = new PaymentInformation(
                            "",
                            0f,
                            0f,
                            "",
                            "membership",
                            "membership"
                    );
                    booking.markAsScheduled(paymentInformation);
                    return booking;
                });
    }

    @Override
    public Either<UseCaseError, Booking> book(Office office, String renterEmail, BookingInformation info) {
        return membershipAcquisitionRepo
                .findById(membershipAcquisitionId)
                .toEither((UseCaseError) MembershipAcquisitionError.MEMBERSHIP_ACQUISITION_NOT_FOUND)
                .filterOrElse(
                        membershipAcquisition -> membershipAcquisition.buyerEmail().equals(renterEmail),
                        m -> MembershipAcquisitionError.MEMBERSHIP_ACQUISITION_FORBIDDEN)
                .filterOrElse(
                        membershipAcquisition -> membershipAcquisition.isActive(
                                        LocalDate.now(Clock.systemUTC()).getMonth(),
                                        ZonedDateTime.of(info.getStartTime(), timezoneARG).toLocalDate()
                                ),
                        m -> MembershipAcquisitionError.MEMBERSHIP_ACQUISITION_IS_NOT_ACTIVE)
                .flatMap(m -> createBooking(office, renterEmail, info));
    }
}

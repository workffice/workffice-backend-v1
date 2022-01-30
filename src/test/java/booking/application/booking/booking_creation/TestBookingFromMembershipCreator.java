package booking.application.booking.booking_creation;

import booking.application.booking.creation.BookingFromMembershipCreator;
import booking.application.dto.booking.BookingError;
import booking.application.dto.booking.BookingInformation;
import booking.application.dto.booking.BookingResponse;
import booking.application.dto.membership_acquisition.MembershipAcquisitionError;
import booking.domain.booking.Booking;
import booking.domain.booking.BookingRepository;
import booking.domain.booking.PaymentInformation;
import booking.domain.booking.Status;
import booking.domain.membership_acquisiton.MembershipAcquisitionId;
import booking.domain.membership_acquisiton.MembershipAcquisitionRepository;
import booking.domain.office.Office;
import booking.domain.office.privacy.PrivateOffice;
import booking.factories.BookingBuilder;
import booking.factories.MembershipAcquisitionBuilder;
import booking.factories.OfficeBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.vavr.control.Either;
import io.vavr.control.Option;
import shared.application.UseCaseError;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestBookingFromMembershipCreator {
    ZoneId timezoneARG = ZoneId.of("America/Argentina/Buenos_Aires");
    BookingRepository bookingRepo = mock(BookingRepository.class);
    MembershipAcquisitionRepository membershipAcquisitionRepo = mock(MembershipAcquisitionRepository.class);
    MembershipAcquisitionId membershipAcquisitionId = new MembershipAcquisitionId();
    PaymentInformation paymentInformationExample = new PaymentInformation(
            "1",
            100f,
            1f,
            "ARS",
            "credit_card",
            "visa"
    );

    BookingFromMembershipCreator creator = new BookingFromMembershipCreator(
            bookingRepo,
            membershipAcquisitionRepo,
            membershipAcquisitionId
    );

    @Test
    void itShouldReturnMembershipAcquisitionNotFoundWhenItDoesNotExist() {
        var office = new OfficeBuilder().build();
        BookingInformation info = BookingInformation.of(
                10,
                LocalDateTime.of(2018, 12, 8, 12, 0, 0),
                LocalDateTime.of(2018, 12, 8, 14, 0, 0)
        );
        when(membershipAcquisitionRepo.findById(membershipAcquisitionId)).thenReturn(Option.none());

        Either<UseCaseError, Booking> response = creator.book(office, "john@wick.com", info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(MembershipAcquisitionError.MEMBERSHIP_ACQUISITION_NOT_FOUND);
    }

    @Test
    void itShouldReturnForbiddenWhenMembershipBuyerEmailIsNotTheSameAsProposedRenter() {
        var office = new OfficeBuilder().build();
        BookingInformation info = BookingInformation.of(
                10,
                LocalDateTime.of(2018, 12, 8, 12, 0, 0),
                LocalDateTime.of(2018, 12, 8, 14, 0, 0)
        );
        var membershipAcquisition = new MembershipAcquisitionBuilder()
                .withId(membershipAcquisitionId)
                .withBuyerEmail("napoleon@mail.com")
                .build();
        when(membershipAcquisitionRepo.findById(membershipAcquisitionId)).thenReturn(Option.of(membershipAcquisition));

        Either<UseCaseError, Booking> response = creator.book(office, "john@wick.com", info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(MembershipAcquisitionError.MEMBERSHIP_ACQUISITION_FORBIDDEN);
    }

    @Test
    void itShouldReturnNotActiveWhenMembershipAcquisitionIsNotFromCurrentMonth() {
        var office = new OfficeBuilder().build();
        BookingInformation info = BookingInformation.of(
                10,
                LocalDateTime.of(2018, 12, 8, 12, 0, 0),
                LocalDateTime.of(2018, 12, 8, 14, 0, 0)
        );
        var membershipAcquisition = new MembershipAcquisitionBuilder()
                .withId(membershipAcquisitionId)
                .withBuyerEmail("napoleon@mail.com")
                .withMonth(LocalDate.now(Clock.systemUTC()).getMonth().minus(3))
                .build();
        when(membershipAcquisitionRepo.findById(membershipAcquisitionId)).thenReturn(Option.of(membershipAcquisition));

        Either<UseCaseError, Booking> response = creator.book(office, "napoleon@mail.com", info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(MembershipAcquisitionError.MEMBERSHIP_ACQUISITION_IS_NOT_ACTIVE);
    }

    @Test
    void itShouldReturnNotActiveWhenProposedScheduleDateIsAnAccessDayOfMembership() {
        var office = new OfficeBuilder().build();
        BookingInformation info = BookingInformation.of(
                10,
                LocalDateTime.of(2021, 12, 7, 12, 0, 0), // Is tuesday
                LocalDateTime.of(2021, 12, 7, 14, 0, 0)
        );
        var membershipAcquisition = new MembershipAcquisitionBuilder()
                .withId(membershipAcquisitionId)
                .withBuyerEmail("napoleon@mail.com")
                .withAccessDays(ImmutableSet.of(DayOfWeek.MONDAY))
                .withMonth(LocalDate.now(Clock.systemUTC()).getMonth())
                .build();
        when(membershipAcquisitionRepo.findById(membershipAcquisitionId)).thenReturn(Option.of(membershipAcquisition));

        Either<UseCaseError, Booking> response = creator.book(office, "napoleon@mail.com", info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(MembershipAcquisitionError.MEMBERSHIP_ACQUISITION_IS_NOT_ACTIVE);
    }

    @Test
    void itShouldReturnNotActiveWhenMembershipAcquisitionIsNotBought() {
        var office = new OfficeBuilder().build();
        BookingInformation info = BookingInformation.of(
                10,
                LocalDateTime.of(2021, 12, 6, 12, 0, 0),
                LocalDateTime.of(2021, 12, 6, 14, 0, 0)
        );
        var membershipAcquisition = new MembershipAcquisitionBuilder()
                .withId(membershipAcquisitionId)
                .withBuyerEmail("napoleon@mail.com")
                .withAccessDays(ImmutableSet.of(DayOfWeek.MONDAY))
                .withMonth(LocalDate.now(Clock.systemUTC()).getMonth())
                .build();
        when(membershipAcquisitionRepo.findById(membershipAcquisitionId)).thenReturn(Option.of(membershipAcquisition));

        Either<UseCaseError, Booking> response = creator.book(office, "napoleon@mail.com", info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(MembershipAcquisitionError.MEMBERSHIP_ACQUISITION_IS_NOT_ACTIVE);
    }

    @Test
    void itShouldReturnInvalidScheduleTimeWhenScheduleTimeProvidedIsInvalid() {
        var office = new OfficeBuilder().build();
        BookingInformation info = BookingInformation.of(
                10,
                LocalDateTime.of(2021, 12, 6, 14, 0, 0),
                LocalDateTime.of(2021, 12, 6, 12, 0, 0)
        );
        var membershipAcquisition = new MembershipAcquisitionBuilder()
                .withId(membershipAcquisitionId)
                .withAccessDays(ImmutableSet.of(DayOfWeek.MONDAY))
                .withBuyerEmail("napoleon@mail.com")
                .withMonth(LocalDate.now(Clock.systemUTC()).getMonth())
                .build();
        membershipAcquisition.buy(paymentInformationExample);
        when(membershipAcquisitionRepo.findById(membershipAcquisitionId)).thenReturn(Option.of(membershipAcquisition));

        Either<UseCaseError, Booking> response = creator.book(office, "napoleon@mail.com", info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(BookingError.INVALID_SCHEDULE_TIME);
    }

    @Test
    void itShouldReturnOfficeIsNotAvailableWhenOfficeIsUnavailableOrThereIsABookingConflict() {
        var office = new OfficeBuilder()
                .withPrivacy(new PrivateOffice(1))
                .build();
        var membershipAcquisition = new MembershipAcquisitionBuilder()
                .withId(membershipAcquisitionId)
                .withAccessDays(ImmutableSet.of(DayOfWeek.MONDAY))
                .withBuyerEmail("napoleon@mail.com")
                .withMonth(LocalDate.now(Clock.systemUTC()).getMonth())
                .build();
        membershipAcquisition.buy(paymentInformationExample);
        when(membershipAcquisitionRepo.findById(membershipAcquisitionId)).thenReturn(Option.of(membershipAcquisition));
        var existentBooking = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 6,
                        14, 0, 0, 0,
                        timezoneARG))
                .withStatus(Status.SCHEDULED)
                .build();
        when(bookingRepo.find(any(Office.class), any(LocalDate.class)))
                .thenReturn(ImmutableList.of(existentBooking));
        BookingInformation info = BookingInformation.of(
                10,
                LocalDateTime.of(2021, 12, 6, 14, 0, 0),
                LocalDateTime.of(2021, 12, 6, 15, 0, 0)
        );

        Either<UseCaseError, Booking> response = creator.book(office, "napoleon@mail.com", info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(BookingError.OFFICE_IS_NOT_AVAILABLE);
    }

    @Test
    void itShouldReturnBookingWithInfoSpecified() {
        var office = new OfficeBuilder().build();
        BookingInformation info = BookingInformation.of(
                10,
                LocalDateTime.of(2021, 12, 6, 12, 0, 0),
                LocalDateTime.of(2021, 12, 6, 13, 0, 0)
        );
        var membershipAcquisition = new MembershipAcquisitionBuilder()
                .withId(membershipAcquisitionId)
                .withAccessDays(ImmutableSet.of(DayOfWeek.MONDAY))
                .withBuyerEmail("napoleon@mail.com")
                .withMonth(LocalDate.now(Clock.systemUTC()).getMonth())
                .build();
        membershipAcquisition.buy(paymentInformationExample);
        when(membershipAcquisitionRepo.findById(membershipAcquisitionId)).thenReturn(Option.of(membershipAcquisition));

        Either<UseCaseError, Booking> response = creator.book(office, "napoleon@mail.com", info);

        assertThat(response.isRight()).isTrue();
        var booking = response.get();
        var freePaymentInformation = BookingResponse.PaymentInformation.of(
                null, // It does not have id until store in the db
                "",
                0f,
                0f,
                "",
                "membership",
                "membership"
        );
        assertThat(booking.toResponse()).isEqualTo(BookingResponse.of(
                booking.id().toString(),
                Status.SCHEDULED.name(),
                10,
                office.price(),
                booking.toResponse().getCreated(),
                LocalDateTime.of(2021, 12, 6, 12, 0, 0),
                LocalDateTime.of(2021, 12, 6, 13, 0, 0),
                freePaymentInformation,
                office.id().toString(),
                office.name(),
                office.officeBranchId()
        ));
    }
}

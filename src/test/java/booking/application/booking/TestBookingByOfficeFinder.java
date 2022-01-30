package booking.application.booking;

import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.factories.OfficeBranchBuilder;
import booking.application.dto.OfficeError;
import booking.application.dto.booking.BookingError;
import booking.application.dto.booking.BookingResponse;
import booking.domain.booking.BookingRepository;
import booking.domain.booking.Status;
import booking.domain.office.OfficeId;
import booking.domain.office.OfficeRepository;
import booking.factories.BookingBuilder;
import booking.factories.OfficeBuilder;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Either;
import io.vavr.control.Option;
import shared.application.UseCaseError;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestBookingByOfficeFinder {
    OfficeBranchFinder officeBranchFinder = mock(OfficeBranchFinder.class);
    OfficeRepository officeRepo = mock(OfficeRepository.class);
    BookingRepository bookingRepo = mock(BookingRepository.class);

    BookingByOfficeFinder finder = new BookingByOfficeFinder(
            officeBranchFinder,
            officeRepo,
            bookingRepo
    );

    @Test
    void itShouldReturnNotFoundWhenOfficeDoesNotExist() {
        var officeId = new OfficeId();
        when(officeRepo.findById(officeId)).thenReturn(Option.none());

        Either<UseCaseError, List<BookingResponse>> response = finder.find(officeId, LocalDate.now());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeError.OFFICE_NOT_FOUND);
    }

    @Test
    void itShouldReturnForbiddenWhenAuthUserDoesNotHaveAccessToBookings() {
        var office = new OfficeBuilder().build();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        when(officeBranchFinder.findWithAuthorization(
                OfficeBranchId.fromString(office.officeBranchId()),
                Permission.create(Access.READ, Resource.BOOKING)
        )).thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN));

        Either<UseCaseError, List<BookingResponse>> response = finder.find(office.id(), LocalDate.now());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(BookingError.BOOKING_FORBIDDEN);
    }

    @Test
    void itShouldReturnBookingsScheduledRelatedWithOfficeAndScheduledAtSpecifiedDate() {
        var office = new OfficeBuilder().build();
        var officeBranch = new OfficeBranchBuilder().build();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        when(officeBranchFinder.findWithAuthorization(
                OfficeBranchId.fromString(office.officeBranchId()),
                Permission.create(Access.READ, Resource.BOOKING)
        )).thenReturn(Either.right(officeBranch.toResponse()));

        var booking1 = new BookingBuilder()
                .withStatus(Status.SCHEDULED)
                .withOffice(office).build();
        var booking2 = new BookingBuilder()
                .withStatus(Status.SCHEDULED)
                .withOffice(office).build();
        var booking3 = new BookingBuilder()
                .withStatus(Status.PENDING)
                .withOffice(office).build();
        when(bookingRepo.find(any(), eq(LocalDate.of(2018, 12, 8))))
                .thenReturn(ImmutableList.of(booking1, booking2, booking3));

        Either<UseCaseError, List<BookingResponse>> response = finder.find(office.id(), LocalDate.of(2018, 12, 8));

        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).containsExactlyInAnyOrder(
                booking1.toResponse(),
                booking2.toResponse()
        );
    }
}

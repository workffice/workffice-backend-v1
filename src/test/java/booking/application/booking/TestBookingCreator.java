package booking.application.booking;

import authentication.application.AuthUserFinder;
import authentication.application.dto.user.AuthUserResponse;
import booking.application.booking.creation.BookingCreationStrategy;
import booking.application.dto.booking.BookingInformation;
import booking.application.dto.booking.BookingResponse;
import booking.domain.booking.Booking;
import booking.domain.booking.BookingRepository;
import booking.domain.office.Office;
import booking.domain.office.OfficeId;
import booking.domain.office.OfficeRepository;
import booking.factories.BookingBuilder;
import booking.factories.OfficeBuilder;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import shared.application.UseCaseError;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static authentication.application.dto.user.UserError.USER_NOT_FOUND;
import static booking.application.dto.OfficeError.OFFICE_IS_DELETED;
import static booking.application.dto.OfficeError.OFFICE_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestBookingCreator {
    AuthUserFinder authUserFinder = mock(AuthUserFinder.class);
    OfficeRepository officeRepo = mock(OfficeRepository.class);
    BookingRepository bookingRepo = mock(BookingRepository.class);
    BookingCreationStrategy bookingCreationStrategy = mock(BookingCreationStrategy.class);
    ArgumentCaptor<Booking> bookingArgumentCaptor = ArgumentCaptor.forClass(Booking.class);

    BookingInformation info = BookingInformation.of(
            10,
            LocalDateTime.of(2018, 12, 8, 14, 0, 0),
            LocalDateTime.of(2018, 12, 8, 15, 0, 0)
    );
    AuthUserResponse authUserResponse = AuthUserResponse.of(
            "1",
            "john@doe.com",
            "john",
            "doe",
            "Street",
            "Some",
            "RENTER",
            "image.url"
    );
    BookingCreator creator = new BookingCreator(authUserFinder, officeRepo, bookingRepo, bookingCreationStrategy);

    @Test
    void itShouldReturnOfficeNotFoundWhenThereIsNoOfficeWithIdProvided() {
        var officeId = new OfficeId();
        when(officeRepo.findById(officeId)).thenReturn(Option.none());

        Either<UseCaseError, BookingResponse> response = creator.create(officeId, info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OFFICE_NOT_FOUND);
    }

    @Test
    void itShouldReturnOfficeIsDeletedWhenOfficeIsDeleted() {
        var office = new OfficeBuilder().build();
        office.delete();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));

        Either<UseCaseError, BookingResponse> response = creator.create(office.id(), info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OFFICE_IS_DELETED);
    }

    @Test
    void itShouldReturnUserNotFoundWhenThereIsNoAuthUser() {
        var office = new OfficeBuilder().build();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        when(authUserFinder.findAuthenticatedUser()).thenReturn(Option.none());

        Either<UseCaseError, BookingResponse> response = creator.create(office.id(), info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(USER_NOT_FOUND);
    }

    @Test
    void itShouldStoreBookingAndReturnBookingCreated() {
        var office = new OfficeBuilder().build();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        when(authUserFinder.findAuthenticatedUser()).thenReturn(Option.of(authUserResponse));
        when(bookingRepo.store(any())).thenReturn(Try.success(null));
        BookingInformation info = BookingInformation.of(
                10,
                LocalDateTime.of(2018, 12, 8, 15, 0, 0),
                LocalDateTime.of(2018, 12, 8, 16, 0, 0)
        );
        var booking = new BookingBuilder().build();
        when(bookingCreationStrategy.book(any(Office.class), eq(authUserResponse.getEmail()), eq(info)))
                .thenReturn(Either.right(booking));

        Either<UseCaseError, BookingResponse> response = creator.create(office.id(), info);

        assertThat(response.isRight()).isTrue();
        verify(bookingRepo, times(1)).store(bookingArgumentCaptor.capture());
        var bookingStored = bookingArgumentCaptor.getValue();
        assertThat(bookingStored.toResponse()).isEqualTo(booking.toResponse());
    }
}

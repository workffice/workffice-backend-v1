package booking.application.booking;

import authentication.application.AuthUserValidator;
import booking.application.dto.booking.BookingError;
import booking.application.dto.booking.BookingResponse;
import booking.domain.booking.Booking;
import booking.domain.booking.BookingId;
import booking.domain.booking.BookingRepository;
import io.vavr.control.Either;

import java.time.Clock;
import java.time.LocalDate;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class BookingFinder {
    private final BookingRepository bookingRepo;
    private final AuthUserValidator authUserValidator;

    public BookingFinder(BookingRepository bookingRepo, AuthUserValidator authUserValidator) {
        this.bookingRepo       = bookingRepo;
        this.authUserValidator = authUserValidator;
    }

    public Either<BookingError, BookingResponse> find(BookingId id) {
        return bookingRepo.findById(id)
                .toEither(BookingError.BOOKING_NOT_FOUND)
                .filterOrElse(
                        booking -> authUserValidator.isSameUserAsAuthenticated(booking.renterEmail()),
                        b -> BookingError.BOOKING_FORBIDDEN)
                .map(Booking::toResponse);
    }

    public Either<BookingError, Page<BookingResponse>> find(
            String renterEmail,
            boolean currentBookings,
            Pageable pageable
    ) {
        var today = LocalDate.now(Clock.systemUTC());
        if (!authUserValidator.isSameUserAsAuthenticated(renterEmail))
            return Either.left(BookingError.BOOKING_FORBIDDEN);
        var bookings = bookingRepo.find(
                renterEmail,
                currentBookings,
                today,
                (int) pageable.getOffset(),
                pageable.getPageSize()
        );
        var bookingResponses = bookings
                .stream()
                .map(Booking::toResponse)
                .collect(Collectors.toList());
        return Either.right(new PageImpl<>(
                bookingResponses,
                pageable,
                bookingRepo.count(renterEmail, currentBookings, today)
        ));
    }
}

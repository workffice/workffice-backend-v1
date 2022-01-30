package report.application;

import booking.domain.booking.BookingConfirmedEvent;
import report.domain.Booking;
import report.domain.BookingRepository;

import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class BookingReportCreator {
    private final BookingRepository bookingRepo;

    public BookingReportCreator(BookingRepository bookingRepo) {
        this.bookingRepo = bookingRepo;
    }

    @EventListener
    public void create(BookingConfirmedEvent event) {
        var booking = Booking.create(
                event.getBookingId(),
                event.getOfficeBranchId(),
                event.getOfficeId(),
                event.getTransactionAmount(),
                event.getPaymentDate()
        ); ;
        bookingRepo
                .store(booking)
                .onFailure(error -> LoggerFactory.getLogger(getClass()).error(error.toString()));
    }
}

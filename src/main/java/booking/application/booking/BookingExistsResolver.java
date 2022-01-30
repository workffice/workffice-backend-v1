package booking.application.booking;

import booking.domain.booking.BookingRepository;
import booking.domain.office.OfficeId;
import booking.domain.office.OfficeRepository;

import org.springframework.stereotype.Service;

@Service
public class BookingExistsResolver {
    private final OfficeRepository  officeRepo;
    private final BookingRepository bookingRepo;

    public BookingExistsResolver(OfficeRepository officeRepo, BookingRepository bookingRepo) {
        this.officeRepo  = officeRepo;
        this.bookingRepo = bookingRepo;
    }

    public boolean bookingExists(String renterEmail, OfficeId officeId) {
        return officeRepo
                .findById(officeId)
                .map(office -> bookingRepo.exists(renterEmail, office))
                .getOrElse(false);
    }
}

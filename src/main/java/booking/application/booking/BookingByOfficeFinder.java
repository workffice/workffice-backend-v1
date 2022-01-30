package booking.application.booking;

import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import booking.application.dto.OfficeError;
import booking.application.dto.booking.BookingError;
import booking.application.dto.booking.BookingResponse;
import booking.domain.booking.Booking;
import booking.domain.booking.BookingRepository;
import booking.domain.office.OfficeId;
import booking.domain.office.OfficeRepository;
import io.vavr.Tuple;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class BookingByOfficeFinder {
    private final OfficeBranchFinder officeBranchFinder;
    private final OfficeRepository   officeRepo;
    private final BookingRepository  bookingRepo;

    public BookingByOfficeFinder(
            OfficeBranchFinder officeBranchFinder,
            OfficeRepository   officeRepo,
            BookingRepository  bookingRepo
    ) {
        this.officeBranchFinder = officeBranchFinder;
        this.officeRepo         = officeRepo;
        this.bookingRepo        = bookingRepo;
    }

    public Either<UseCaseError, List<BookingResponse>> find(OfficeId officeId, LocalDate scheduledDate) {
        return officeRepo.findById(officeId)
                .toEither((UseCaseError) OfficeError.OFFICE_NOT_FOUND)
                .filterOrElse(
                        office -> officeBranchFinder.findWithAuthorization(
                                OfficeBranchId.fromString(office.officeBranchId()),
                                Permission.create(Access.READ, Resource.BOOKING)
                        ).isRight(), o -> BookingError.BOOKING_FORBIDDEN)
                .map(office -> Tuple.of(office, bookingRepo.find(office, scheduledDate)))
                .map(officeWithBookings -> officeWithBookings._2.stream()
                        .filter(Booking::isScheduled)
                        .map(booking -> booking.toResponse(officeWithBookings._1))
                        .collect(Collectors.toList()));
    }
}

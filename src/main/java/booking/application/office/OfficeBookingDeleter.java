package booking.application.office;

import backoffice.domain.office.OfficeDeletedEvent;
import booking.domain.office.Office;
import booking.domain.office.OfficeId;
import booking.domain.office.OfficeRepository;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class OfficeBookingDeleter {
    private final OfficeRepository officeRepo;

    public OfficeBookingDeleter(OfficeRepository officeRepo) {
        this.officeRepo = officeRepo;
    }

    @EventListener
    public void delete(OfficeDeletedEvent event) {
        officeRepo.findById(OfficeId.fromString(event.getOfficeId()))
                .map(Office::delete)
                .peek(officeRepo::update);
    }
}

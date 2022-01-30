package booking.application.office;

import backoffice.domain.office.OfficeCreatedEvent;
import booking.domain.office.Office;
import booking.domain.office.OfficeId;
import booking.domain.office.OfficeRepository;
import booking.domain.office.privacy.Privacy;
import io.vavr.control.Try;

import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class OfficeBookingCreator {
    private final OfficeRepository officeRepo;

    public OfficeBookingCreator(OfficeRepository officeRepo) {
        this.officeRepo = officeRepo;
    }

    @EventListener
    public void create(OfficeCreatedEvent event) {
        Try<Privacy> privacyOrError = Privacy.createPrivacy(
                event.getPrivacy(),
                event.getCapacity(),
                event.getTablesQuantity(),
                event.getCapacityPerTable()
        );
        privacyOrError.
                map(privacy -> Office.create(
                        OfficeId.fromString(event.getId()),
                        event.getOfficeBranchId(),
                        event.getName(),
                        event.getPrice(),
                        privacy))
                .onSuccess(officeRepo::store)
                .onFailure(error -> LoggerFactory.getLogger(this.getClass()).error(error.toString()));
    }
}


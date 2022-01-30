package booking.application.office;

import backoffice.domain.office.OfficeUpdatedEvent;
import booking.domain.office.OfficeId;
import booking.domain.office.OfficeRepository;
import booking.domain.office.privacy.Privacy;
import io.vavr.control.Option;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class OfficeBookingUpdater {
    private final OfficeRepository officeRepo;

    public OfficeBookingUpdater(OfficeRepository officeRepo) {
        this.officeRepo = officeRepo;
    }

    @EventListener
    public void update(OfficeUpdatedEvent event) {
        officeRepo.findById(OfficeId.fromString(event.getId()))
                .flatMap(office -> {
                    var privacyOrError = Privacy.createPrivacy(
                            event.getPrivacy(),
                            event.getCapacity(),
                            event.getTablesQuantity(),
                            event.getCapacityPerTable()
                    );
                    if (privacyOrError.isFailure())
                        return Option.none();
                    return Option.of(office.update(event.getName(), event.getPrice(), privacyOrError.get()));
                }).peek(officeRepo::update);

    }
}

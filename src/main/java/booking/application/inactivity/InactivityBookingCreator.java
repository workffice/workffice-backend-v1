package booking.application.inactivity;

import backoffice.domain.office_inactivity.InactivityCreatedEvent;
import booking.domain.inactivity.Inactivity;
import booking.domain.inactivity.InactivityId;
import booking.domain.inactivity.RecurringDay;
import booking.domain.inactivity.SpecificDate;
import booking.domain.office.OfficeId;
import booking.domain.office.OfficeRepository;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

@Service
public class InactivityBookingCreator {
    private final OfficeRepository officeRepo;

    public InactivityBookingCreator(OfficeRepository officeRepo) {
        this.officeRepo = officeRepo;
    }

    @EventListener
    public void create(InactivityCreatedEvent event) {
        Inactivity inactivity = Match(event.getInactivityType()).of(
                Case($("SPECIFIC_DATE"), () -> new SpecificDate(
                        InactivityId.fromString(event.getInactivityId()),
                        event.getSpecificInactivityDay().get())),
                Case($("RECURRING_DAY"), () -> new RecurringDay(
                        InactivityId.fromString(event.getInactivityId()),
                        event.getDayOfWeek().get()
                ))
        );
        officeRepo.findById(OfficeId.fromString(event.getOfficeId()))
                .map(office -> {
                    office.addInactivity(inactivity);
                    return office;
                }).peek(officeRepo::update);

    }
}

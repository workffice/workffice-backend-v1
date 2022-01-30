package booking.application.inactivity;

import backoffice.domain.office_inactivity.InactivityDeletedEvent;
import booking.domain.inactivity.InactivityId;
import booking.infrastructure.repositories.InactivityMySQLRepository;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class InactivityBookingDeleter {
    public InactivityMySQLRepository inactivityMySQLRepository;

    public InactivityBookingDeleter(InactivityMySQLRepository inactivityMySQLRepository) {
        this.inactivityMySQLRepository = inactivityMySQLRepository;
    }

    @EventListener
    public void deleteInactivity(InactivityDeletedEvent event) {
        inactivityMySQLRepository.delete(InactivityId.fromString(event.getInactivityId()));
    }
}

package shared.infrastructure;

import shared.domain.DomainEvent;
import shared.domain.EventBus;

import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringEventBus implements EventBus {
    
    private final ApplicationEventPublisher eventPublisher;
    
    public SpringEventBus(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    public void publish(DomainEvent event) {
        LoggerFactory.getLogger(this.getClass()).info("Dispatching event: " + event.getEventName());
        eventPublisher.publishEvent(event);
    }
}

package backoffice.application.office_holder;

import authentication.domain.user.UserCreatedEvent;
import backoffice.application.dto.office_holder.OfficeHolderInformation;
import backoffice.domain.office_holder.OfficeHolder;
import backoffice.domain.office_holder.OfficeHolderId;
import backoffice.domain.office_holder.OfficeHolderRepository;

import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class OfficeHolderCreator {
    
    private final OfficeHolderRepository repository;
    
    public OfficeHolderCreator(OfficeHolderRepository repository) {
        this.repository = repository;
    }
    
    @EventListener
    public void createOfficeHolderFromEvent(UserCreatedEvent userCreated) {
        if (userCreated.getUserType().equals("OFFICE_HOLDER"))
            this.createOfficeHolder(
                    new OfficeHolderId(UUID.fromString(userCreated.getId())),
                    new OfficeHolderInformation(userCreated.getEmail())
            );
    }
    
    private void createOfficeHolder(OfficeHolderId id, OfficeHolderInformation newOfficeHolder) {
        OfficeHolder officeHolder = new OfficeHolder(id, newOfficeHolder.getEmail());
        repository.store(officeHolder);
    }
}

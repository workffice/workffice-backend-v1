package backoffice.domain.office_holder;

import shared.domain.DomainId;

import java.util.UUID;

public class OfficeHolderId extends DomainId {
    
    public OfficeHolderId() { super(); }
    
    public OfficeHolderId(UUID id) { super(id); }
    
    public static OfficeHolderId fromString(String id) {
        return new OfficeHolderId(UUID.fromString(id));
    }
}

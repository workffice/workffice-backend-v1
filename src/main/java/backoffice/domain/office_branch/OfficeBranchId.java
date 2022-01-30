package backoffice.domain.office_branch;

import shared.domain.DomainId;

import java.util.UUID;

public class OfficeBranchId extends DomainId {
    public OfficeBranchId() {
        super();
    }
    
    public OfficeBranchId(UUID id) {
        super(id);
    }
    
    public static OfficeBranchId fromString(String id) {
        return new OfficeBranchId(UUID.fromString(id));
    }
}

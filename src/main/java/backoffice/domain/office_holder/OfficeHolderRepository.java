package backoffice.domain.office_holder;

import backoffice.domain.office_branch.OfficeBranch;
import io.vavr.control.Option;

public interface OfficeHolderRepository {
    
    void store(OfficeHolder officeHolder);
    
    Option<OfficeHolder> findById(OfficeHolderId id);
    
    Option<OfficeHolder> findByOfficeBranch(OfficeBranch officeBranch);

    Option<OfficeHolder> find(String email);
}

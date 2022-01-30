package backoffice.domain.office_branch;

import backoffice.domain.office_holder.OfficeHolder;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.List;

public interface OfficeBranchRepository {
    
    Try<Void> store(OfficeBranch officeBranch);

    Try<Void> update(OfficeBranch officeBranch);
    
    Option<OfficeBranch> findById(OfficeBranchId id);

    List<OfficeBranch> findByIds(List<OfficeBranchId> ids);

    List<OfficeBranch> findByOfficeHolder(OfficeHolder officeHolder);
}

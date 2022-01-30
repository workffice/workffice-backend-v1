package backoffice.domain.office;

import backoffice.domain.office_branch.OfficeBranch;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.List;

public interface OfficeRepository {
    
    Try<Void> store(Office office);

    Try<Void> update(Office office);
    
    Option<Office> findById(OfficeId id);

    List<Office> findByOfficeBranch(OfficeBranch officeBranch);
}

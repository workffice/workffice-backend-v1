package backoffice.domain.service;

import backoffice.domain.office_branch.OfficeBranch;
import io.vavr.control.Try;

import java.util.List;

public interface ServiceRepository {

    Try<Void> store (Service service);

    List<Service> findByOfficeBranch(OfficeBranch officeBranch);
}

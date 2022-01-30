package backoffice.domain.collaborator;

import backoffice.domain.office_branch.OfficeBranch;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.List;

public interface CollaboratorRepository {

    Try<Void> store(Collaborator collaborator);

    Try<Void> update(Collaborator collaborator);

    Option<Collaborator> findById(CollaboratorId id);

    boolean exists(String email, OfficeBranch officeBranch);

    Option<Collaborator> findWithRoles(CollaboratorId id);

    List<Collaborator> find(OfficeBranch officeBranch);

    List<Collaborator> find(String email);

    Option<Collaborator> find(String email, OfficeBranch officeBranch);
}

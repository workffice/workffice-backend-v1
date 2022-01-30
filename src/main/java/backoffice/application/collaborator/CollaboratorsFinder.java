package backoffice.application.collaborator;

import backoffice.application.dto.collaborator.CollaboratorResponse;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.collaborator.Collaborator;
import backoffice.domain.collaborator.CollaboratorRepository;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CollaboratorsFinder {

    private final OfficeBranchFinder officeBranchFinder;
    private final CollaboratorRepository collaboratorRepo;

    public CollaboratorsFinder(
            OfficeBranchFinder officeBranchFinder,
            CollaboratorRepository collaboratorRepo
    ) {
        this.officeBranchFinder = officeBranchFinder;
        this.collaboratorRepo = collaboratorRepo;
    }

    private List<CollaboratorResponse> toCollaboratorResponses(List<Collaborator> collaborators) {
        return collaborators.stream().map(Collaborator::toResponse).collect(Collectors.toList());
    }

    public Either<UseCaseError, List<CollaboratorResponse>> find(OfficeBranchId officeBranchId) {
        return officeBranchFinder
                .findWithAuthorization(officeBranchId, Permission.create(Access.READ, Resource.COLLABORATOR))
                .map(OfficeBranch::fromDTO)
                .map(collaboratorRepo::find)
                .map(this::toCollaboratorResponses);
    }
}

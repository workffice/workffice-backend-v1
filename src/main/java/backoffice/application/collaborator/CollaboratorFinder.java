package backoffice.application.collaborator;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.collaborator.CollaboratorError;
import backoffice.application.dto.collaborator.CollaboratorResponse;
import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.domain.collaborator.Collaborator;
import backoffice.domain.collaborator.CollaboratorId;
import backoffice.domain.collaborator.CollaboratorRepository;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import org.springframework.stereotype.Service;

@Service
public class CollaboratorFinder {
    private final PermissionValidator    permissionValidator;
    private final CollaboratorRepository collaboratorRepo;

    public CollaboratorFinder(
            PermissionValidator    permissionValidator,
            CollaboratorRepository collaboratorRepo
    ) {
        this.permissionValidator = permissionValidator;
        this.collaboratorRepo    = collaboratorRepo;
    }

    public Either<UseCaseError, CollaboratorResponse> find(CollaboratorId id) {
        return collaboratorRepo
                .findById(id)
                .toEither((UseCaseError) CollaboratorError.COLLABORATOR_NOT_FOUND)
                .filterOrElse(
                        collaborator -> permissionValidator.userHasPerms(
                                collaborator.officeBranch(),
                                Permission.create(Access.READ, Resource.COLLABORATOR)
                        ), c -> OfficeBranchError.OFFICE_BRANCH_FORBIDDEN)
                .map(Collaborator::toResponse);
    }
}

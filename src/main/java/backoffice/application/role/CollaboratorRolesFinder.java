package backoffice.application.role;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.collaborator.CollaboratorError;
import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.dto.role.RoleResponse;
import backoffice.domain.collaborator.Collaborator;
import backoffice.domain.collaborator.CollaboratorId;
import backoffice.domain.collaborator.CollaboratorRepository;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.domain.role.Role;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CollaboratorRolesFinder {
    private final PermissionValidator    permissionValidator;
    private final CollaboratorRepository collaboratorRepo;

    public CollaboratorRolesFinder(
            PermissionValidator    permissionValidator,
            CollaboratorRepository collaboratorRepo
    ) {
        this.permissionValidator = permissionValidator;
        this.collaboratorRepo    = collaboratorRepo;
    }

    public Either<UseCaseError, List<RoleResponse>> find(CollaboratorId collaboratorId) {
        return collaboratorRepo
                .findWithRoles(collaboratorId)
                .toEither((UseCaseError) CollaboratorError.COLLABORATOR_NOT_FOUND)
                .filterOrElse(
                        collaborator -> permissionValidator.userHasPerms(
                                collaborator.officeBranch(),
                                Permission.create(Access.READ, Resource.COLLABORATOR)
                        ), c -> OfficeBranchError.OFFICE_BRANCH_FORBIDDEN)
                .map(Collaborator::roles)
                .map(roles -> roles.stream().map(Role::toResponse).collect(Collectors.toList()));
    }
}

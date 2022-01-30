package backoffice.application.collaborator;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.collaborator.CollaboratorError;
import backoffice.application.dto.collaborator.CollaboratorUpdateInformation;
import backoffice.domain.collaborator.CollaboratorId;
import backoffice.domain.collaborator.CollaboratorRepository;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.domain.role.Role;
import backoffice.domain.role.RoleId;
import backoffice.domain.role.RoleRepository;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CollaboratorUpdater {
    private final PermissionValidator    permissionValidator;
    private final RoleRepository         roleRepo;
    private final CollaboratorRepository collaboratorRepo;

    public CollaboratorUpdater(
            PermissionValidator    permissionValidator,
            RoleRepository         roleRepo,
            CollaboratorRepository collaboratorRepo
    ) {
        this.permissionValidator = permissionValidator;
        this.roleRepo            = roleRepo;
        this.collaboratorRepo    = collaboratorRepo;
    }

    private Set<Role> findRoles(OfficeBranch officeBranch, Set<UUID> ids) {
        Set<RoleId> roleIds = ids.stream().map(RoleId::new).collect(Collectors.toSet());
        return roleRepo.findByOfficeBranch(officeBranch)
                .stream()
                .filter(role -> roleIds.contains(role.id()))
                .collect(Collectors.toSet());
    }

    public Either<UseCaseError, Void> update(CollaboratorId collaboratorId, CollaboratorUpdateInformation info) {
        return collaboratorRepo
                .findById(collaboratorId)
                .toEither((UseCaseError) CollaboratorError.COLLABORATOR_NOT_FOUND)
                .filterOrElse(
                        collaborator -> permissionValidator.userHasPerms(
                                collaborator.officeBranch(),
                                Permission.create(Access.WRITE, Resource.COLLABORATOR)
                        ), c -> CollaboratorError.FORBIDDEN)
                .map(collaborator -> collaborator
                        .copy(info.getName(), findRoles(collaborator.officeBranch(), info.getRoleIds())))
                .flatMap(collaborator -> collaboratorRepo.update(collaborator).toEither(CollaboratorError.DB_ERROR));

    }
}

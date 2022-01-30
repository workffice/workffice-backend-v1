package backoffice.application.collaborator;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.collaborator.CollaboratorError;
import backoffice.domain.collaborator.CollaboratorId;
import backoffice.domain.collaborator.CollaboratorRepository;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import io.vavr.control.Either;

import org.springframework.stereotype.Service;

@Service
public class CollaboratorDeleter {
    private final CollaboratorRepository collaboratorRepo;
    private final PermissionValidator    permissionValidator;

    public CollaboratorDeleter(
            CollaboratorRepository collaboratorRepo,
            PermissionValidator    permissionValidator
    ) {
        this.collaboratorRepo    = collaboratorRepo;
        this.permissionValidator = permissionValidator;
    }

    public Either<CollaboratorError, Void> delete(CollaboratorId id) {
        return collaboratorRepo.findById(id)
                .toEither(CollaboratorError.COLLABORATOR_NOT_FOUND)
                .filterOrElse(
                        collaborator -> permissionValidator.userHasPerms(
                                collaborator.officeBranch(),
                                Permission.create(Access.WRITE, Resource.COLLABORATOR)
                        ), c -> CollaboratorError.FORBIDDEN
                )
                .map(collaborator -> {
                    collaborator.delete();
                    return collaborator;
                }).flatMap(collaborator -> collaboratorRepo
                        .update(collaborator)
                        .toEither(CollaboratorError.DB_ERROR)
                );
    }
}

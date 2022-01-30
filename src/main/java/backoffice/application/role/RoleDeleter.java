package backoffice.application.role;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.role.RoleError;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.domain.role.RoleId;
import backoffice.domain.role.RoleRepository;
import io.vavr.control.Either;

import org.springframework.stereotype.Service;

@Service
public class RoleDeleter {
    private final RoleRepository      roleRepo;
    private final PermissionValidator permissionValidator;

    public RoleDeleter(RoleRepository roleRepo, PermissionValidator permissionValidator) {
        this.roleRepo            = roleRepo;
        this.permissionValidator = permissionValidator;
    }

    public Either<RoleError, Void> delete(RoleId id) {
        return roleRepo.findById(id)
                .toEither(RoleError.ROLE_NOT_FOUND)
                .filterOrElse(
                        role -> permissionValidator.userHasPerms(
                                role.officeBranch(),
                                Permission.create(Access.WRITE, Resource.ROLE)
                        ), r -> RoleError.ROLE_FORBIDDEN)
                .map(role -> {
                    role.markAsDeleted();
                    return role;
                })
                .flatMap(role -> roleRepo.update(role).toEither(RoleError.DB_ERROR));
    }
}

package backoffice.application.role;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.role.RoleError;
import backoffice.application.dto.role.RoleInformation;
import backoffice.application.dto.role.RoleResponse;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.domain.role.Role;
import backoffice.domain.role.RoleId;
import backoffice.domain.role.RoleRepository;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RoleUpdater {

    private final RoleRepository      roleRepository;
    private final PermissionValidator permissionValidator;

    public RoleUpdater(RoleRepository roleRepository, PermissionValidator permissionValidator) {
        this.roleRepository      = roleRepository;
        this.permissionValidator = permissionValidator;
    }

    private Role updateRole(Role role, RoleInformation information) {
        Set<Permission> permissions = information.getPermissions()
                .stream()
                .map(perm -> Permission.create(Access.valueOf(perm.getAccess()), Resource.valueOf(perm.getResource())))
                .collect(Collectors.toSet());
        return Role.create(role.id(), information.getName(), permissions, role.officeBranch());
    }

    public Either<UseCaseError, RoleResponse> update(RoleId id, RoleInformation roleInformation) {
        var roleWithNewPropsOrError = roleRepository.findById(id)
                .toEither((UseCaseError) RoleError.ROLE_NOT_FOUND)
                .filterOrElse(
                        role -> permissionValidator.userHasPerms(
                                role.officeBranch(),
                                Permission.create(Access.WRITE, Resource.ROLE)
                        ), role -> RoleError.ROLE_FORBIDDEN)
                .map(role -> updateRole(role, roleInformation));

        if (roleWithNewPropsOrError.isLeft())
            return Either.left(roleWithNewPropsOrError.getLeft());

        var roleWithNewProps = roleWithNewPropsOrError.get();
        return roleRepository
                .update(roleWithNewProps)
                .toEither((UseCaseError) RoleError.DB_ERROR)
                .map(v -> roleWithNewProps.toResponse());
    }
}

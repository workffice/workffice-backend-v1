package backoffice.application.role;

import backoffice.application.dto.role.RoleInformation;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
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

import static backoffice.application.dto.role.RoleError.DB_ERROR;

@Service
public class RoleCreator {
    private final RoleRepository     roleRepo;
    private final OfficeBranchFinder officeBranchFinder;

    public RoleCreator(
            RoleRepository     roleRepo,
            OfficeBranchFinder officeBranchFinder
    ) {
        this.roleRepo           = roleRepo;
        this.officeBranchFinder = officeBranchFinder;
    }

    private Role createRole(RoleId id, RoleInformation info, OfficeBranch officeBranch) {
        Set<Permission> permissions = info.getPermissions()
                .stream()
                .map(perm -> Permission.create(Access.valueOf(perm.getAccess()),
                        Resource.valueOf(perm.getResource())))
                .collect(Collectors.toSet());
        return Role.create(id, info.getName(), permissions, officeBranch);
    }

    public Either<UseCaseError, Void> createRole(OfficeBranchId officeBranchId, RoleId id, RoleInformation info) {
        return officeBranchFinder
                .findWithAuthorization(officeBranchId, Permission.create(Access.WRITE, Resource.ROLE))
                .map(OfficeBranch::fromDTO)
                .map(officeBranch -> this.createRole(id, info, officeBranch))
                .flatMap(role -> roleRepo.store(role).toEither(DB_ERROR));
    }
}

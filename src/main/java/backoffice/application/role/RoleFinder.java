package backoffice.application.role;

import backoffice.application.dto.role.RoleResponse;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.domain.role.Role;
import backoffice.domain.role.RoleRepository;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RoleFinder {
    private final RoleRepository     roleRepo;
    private final OfficeBranchFinder officeBranchFinder;

    public RoleFinder(RoleRepository roleRepo, OfficeBranchFinder officeBranchFinder) {
        this.roleRepo           = roleRepo;
        this.officeBranchFinder = officeBranchFinder;
    }

    private List<RoleResponse> toRoleResponses(List<Role> roles) {
        return roles
                .stream()
                .map(Role::toResponse)
                .collect(Collectors.toList());
    }

    public Either<UseCaseError, List<RoleResponse>> findRoles(OfficeBranchId officeBranchId) {
        return officeBranchFinder
                .findWithAuthorization(officeBranchId, Permission.create(Access.READ, Resource.ROLE))
                .map(officeBranch -> roleRepo.findByOfficeBranch(OfficeBranch.fromDTO(officeBranch)))
                .map(this::toRoleResponses);
    }
}

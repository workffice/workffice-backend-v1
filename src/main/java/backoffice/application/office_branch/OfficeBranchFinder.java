package backoffice.application.office_branch;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.office_branch.OfficeBranchResponse;
import backoffice.application.dto.office_holder.OfficeHolderError;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.office_branch.OfficeBranchRepository;
import backoffice.domain.office_holder.OfficeHolderId;
import backoffice.domain.office_holder.OfficeHolderRepository;
import backoffice.domain.role.Permission;
import io.vavr.control.Either;
import io.vavr.control.Option;
import shared.application.UseCaseError;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

import static backoffice.application.dto.office_branch.OfficeBranchError.OFFICE_BRANCH_FORBIDDEN;
import static backoffice.application.dto.office_branch.OfficeBranchError.OFFICE_BRANCH_NOT_EXIST;

@Service
public class OfficeBranchFinder {

    private final PermissionValidator    permissionValidator;
    private final OfficeBranchRepository officeBranchRepo;
    private final OfficeHolderRepository officeHolderRepo;

    public OfficeBranchFinder(
            PermissionValidator    permissionValidator,
            OfficeBranchRepository officeBranchRepo,
            OfficeHolderRepository officeHolderRepo
    ) {
        this.officeHolderRepo    = officeHolderRepo;
        this.officeBranchRepo    = officeBranchRepo;
        this.permissionValidator = permissionValidator;
    }

    private List<OfficeBranchResponse> toOfficeBranchResponses(List<OfficeBranch> officeBranches) {
        return officeBranches
                .stream()
                .map(OfficeBranch::toResponse)
                .collect(Collectors.toList());
    }

    public Either<UseCaseError, OfficeBranchResponse> findWithAuthorization(
            OfficeBranchId id,
            Permission permission
    ) {
        return officeBranchRepo.findById(id)
                .toEither((UseCaseError) OFFICE_BRANCH_NOT_EXIST)
                .filterOrElse(
                        officeBranch -> permissionValidator.userHasPerms(officeBranch, permission),
                        officeBranch -> OFFICE_BRANCH_FORBIDDEN)
                .map(OfficeBranch::toResponse);
    }

    public Option<OfficeBranchResponse> find(OfficeBranchId id) {
        return officeBranchRepo
                .findById(id)
                .map(OfficeBranch::toResponse);
    }

    public Either<OfficeHolderError, List<OfficeBranchResponse>> findByOfficeHolder(OfficeHolderId id) {
        return officeHolderRepo
                .findById(id)
                .toEither(OfficeHolderError.OFFICE_HOLDER_NOT_FOUND)
                .map(officeBranchRepo::findByOfficeHolder)
                .map(this::toOfficeBranchResponses);
    }
}

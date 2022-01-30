package backoffice.application.service;

import backoffice.application.dto.service.ServiceError;
import backoffice.application.dto.service.ServiceInformation;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.domain.service.Service;
import backoffice.domain.service.ServiceCategory;
import backoffice.domain.service.ServiceId;
import backoffice.domain.service.ServiceRepository;
import io.vavr.control.Either;
import shared.application.UseCaseError;

@org.springframework.stereotype.Service
public class ServiceCreator {

    private final ServiceRepository officeBranchServiceRepo;
    private final OfficeBranchFinder officeBranchFinder;

    public ServiceCreator(
            ServiceRepository officeBranchServiceRepo,
            OfficeBranchFinder officeBranchFinder) {
        this.officeBranchServiceRepo = officeBranchServiceRepo;
        this.officeBranchFinder = officeBranchFinder;
    }

    private Service createService(
            ServiceId id,
            ServiceInformation info,
            OfficeBranch officeBranch
    ) {
        return Service.create(
                id,
                info.getName(),
                ServiceCategory.valueOf(info.getCategory()),
                officeBranch);
    }

    public Either<UseCaseError, Void> createService(
            ServiceId id,
            ServiceInformation info,
            OfficeBranchId officeBranchId) {
        return officeBranchFinder
                .findWithAuthorization(officeBranchId, Permission.create(Access.WRITE, Resource.SERVICE))
                .map(OfficeBranch::fromDTO)
                .map(officeBranch -> createService(id, info, officeBranch))
                .flatMap(officeBranchService -> officeBranchServiceRepo
                        .store(officeBranchService)
                        .toEither(ServiceError.DB_ERROR));
    }
}


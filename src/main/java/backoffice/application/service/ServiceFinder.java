package backoffice.application.service;

import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.dto.service.ServiceResponse;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.service.Service;
import backoffice.domain.service.ServiceRepository;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class ServiceFinder {

    private final ServiceRepository officeBranchServiceRepo;
    private final OfficeBranchFinder officeBranchFinder;

    public ServiceFinder(ServiceRepository officeBranchServiceRepo, OfficeBranchFinder officeBranchFinder) {
        this.officeBranchServiceRepo = officeBranchServiceRepo;
        this.officeBranchFinder = officeBranchFinder;
    }

    private List<ServiceResponse> toOfficeBranchServiceResponse(List<Service> services) {
        return services.stream()
                .map(Service::toResponse)
                .collect(Collectors.toList());
    }

    public Either<UseCaseError, List<ServiceResponse>> find(OfficeBranchId officeBranchId) {
        return officeBranchFinder
                .find(officeBranchId)
                .toEither((UseCaseError) OfficeBranchError.OFFICE_BRANCH_NOT_EXIST)
                .map(OfficeBranch::fromDTO)
                .map(officeBranchServiceRepo::findByOfficeBranch)
                .map(this::toOfficeBranchServiceResponse);
    }
}

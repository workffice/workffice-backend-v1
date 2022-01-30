package backoffice.application.office;

import backoffice.application.dto.office.OfficeResponse;
import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.office.Office;
import backoffice.domain.office.OfficeRepository;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OfficesFinder {

    private final OfficeRepository   officeRepo;
    private final OfficeBranchFinder officeBranchFinder;

    public OfficesFinder(OfficeRepository officeRepo, OfficeBranchFinder officeBranchFinder) {
        this.officeRepo         = officeRepo;
        this.officeBranchFinder = officeBranchFinder;
    }

    private List<OfficeResponse> toOfficeResponses(List<Office> offices) {
        return offices.stream()
                .map(Office::toResponse)
                .collect(Collectors.toList());
    }

    public Either<UseCaseError, List<OfficeResponse>> find(OfficeBranchId officeBranchId) {
        return officeBranchFinder
                .find(officeBranchId)
                .toEither((UseCaseError) OfficeBranchError.OFFICE_BRANCH_NOT_EXIST)
                .map(OfficeBranch::fromDTO)
                .map(officeRepo::findByOfficeBranch)
                .map(offices -> {
                    var officesNonDeleted = offices
                            .stream()
                            .filter(office -> !office.isDeleted(LocalDate.now(Clock.systemUTC())))
                            .collect(Collectors.toList());
                    return toOfficeResponses(officesNonDeleted);
                });
    }
}

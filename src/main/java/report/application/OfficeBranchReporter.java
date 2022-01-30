package report.application;

import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import io.vavr.control.Either;
import report.domain.BookingRepository;
import report.domain.OfficeBookedProjection;
import report.domain.OfficeTransactionAmountProjection;
import report.domain.TransactionAmountProjection;
import shared.application.UseCaseError;

import java.time.Month;
import java.time.Year;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class OfficeBranchReporter {
    private final BookingRepository bookingRepo;
    private final OfficeBranchFinder officeBranchFinder;

    public OfficeBranchReporter(BookingRepository bookingRepo, OfficeBranchFinder officeBranchFinder) {
        this.bookingRepo = bookingRepo;
        this.officeBranchFinder = officeBranchFinder;
    }

    private <T> Either<UseCaseError, List<T>> getReport(
            String officeBranchId,
            Supplier<List<T>> getReport
    ) {
        return officeBranchFinder.findWithAuthorization(
                OfficeBranchId.fromString(officeBranchId),
                Permission.create(Access.READ, Resource.REPORT))
                .map(officeBranch -> getReport.get());
    }

    public Either<UseCaseError, List<TransactionAmountProjection>> transactionAmountReport(
            String officeBranchId,
            Year year
    ) {
        return getReport(officeBranchId, () -> bookingRepo.transactionAmountReport(officeBranchId, year));
    }

    public Either<UseCaseError, List<OfficeTransactionAmountProjection>> transactionAmountPerOfficeReport(
            String officeBranchId,
            Month month
    ) {
        return getReport(officeBranchId, () -> bookingRepo.officesTransactionAmountReport(officeBranchId, month));
    }

    public Either<UseCaseError, List<OfficeBookedProjection>> officeBookingsQuantityReport(
            String officeBranchId,
            Month month
    ) {
        return getReport(officeBranchId, () -> bookingRepo.officeBookedReport(officeBranchId, month));
    }
}

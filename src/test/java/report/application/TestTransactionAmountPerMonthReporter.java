package report.application;

import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.factories.OfficeBranchBuilder;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Either;
import report.domain.BookingRepository;
import report.domain.TransactionAmountProjection;
import shared.application.UseCaseError;

import java.time.Year;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestTransactionAmountPerMonthReporter {
    BookingRepository bookingRepo = mock(BookingRepository.class);
    OfficeBranchFinder officeBranchFinder = mock(OfficeBranchFinder.class);

    OfficeBranchReporter reporter = new OfficeBranchReporter(
            bookingRepo,
            officeBranchFinder
    );

    @Test
    void itShouldReturnNotFoundWhenOfficeBranchNotExist() {
        var officeBranchId = new OfficeBranchId();
        when(officeBranchFinder.findWithAuthorization(
                officeBranchId,
                Permission.create(Access.READ, Resource.REPORT)
        )).thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST));

        Either<UseCaseError, List<TransactionAmountProjection>> response = reporter
                .transactionAmountReport(officeBranchId.toString(), Year.of(2021));

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST);
    }

    @Test
    void itShouldReturnForbiddenWhenAuthUserDoesNotHaveAccessToReports() {
        var officeBranchId = new OfficeBranchId();
        when(officeBranchFinder.findWithAuthorization(
                officeBranchId,
                Permission.create(Access.READ, Resource.REPORT)
        )).thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN));

        Either<UseCaseError, List<TransactionAmountProjection>> response = reporter
                .transactionAmountReport(officeBranchId.toString(), Year.of(2021));

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN);
    }

    @Test
    void itShouldReturnOfficesWithTotalBookings() {
        var officeBranch = new OfficeBranchBuilder().build();
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.READ, Resource.REPORT)
        )).thenReturn(Either.right(officeBranch.toResponse()));
        when(bookingRepo.transactionAmountReport(officeBranch.id().toString(), Year.of(2021)))
                .thenReturn(ImmutableList.of(
                        TransactionAmountProjection.of(2021, "SEPTEMBER", 200f),
                        TransactionAmountProjection.of(2021, "AUGUST", 2200f),
                        TransactionAmountProjection.of(2021, "JUNE", 1200f)
                ));

        Either<UseCaseError, List<TransactionAmountProjection>> response = reporter
                .transactionAmountReport(officeBranch.id().toString(), Year.of(2021));

        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).containsExactly(
                TransactionAmountProjection.of(2021, "SEPTEMBER", 200f),
                TransactionAmountProjection.of(2021, "AUGUST", 2200f),
                TransactionAmountProjection.of(2021, "JUNE", 1200f)
        );
    }
}

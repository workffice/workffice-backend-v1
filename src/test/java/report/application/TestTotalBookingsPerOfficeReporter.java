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
import report.domain.OfficeBookedProjection;
import shared.application.UseCaseError;

import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestTotalBookingsPerOfficeReporter {
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

        Either<UseCaseError, List<OfficeBookedProjection>> response = reporter
                .officeBookingsQuantityReport(officeBranchId.toString(), Month.SEPTEMBER);

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

        Either<UseCaseError, List<OfficeBookedProjection>> response = reporter
                .officeBookingsQuantityReport(officeBranchId.toString(), Month.SEPTEMBER);

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
        when(bookingRepo.officeBookedReport(officeBranch.id().toString(), Month.SEPTEMBER))
                .thenReturn(ImmutableList.of(
                        OfficeBookedProjection.of("1", "SEPTEMBER", 20),
                        OfficeBookedProjection.of("2", "SEPTEMBER", 15),
                        OfficeBookedProjection.of("3", "SEPTEMBER", 5)
                ));

        Either<UseCaseError, List<OfficeBookedProjection>> response = reporter
                .officeBookingsQuantityReport(officeBranch.id().toString(), Month.SEPTEMBER);

        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).containsExactly(
                OfficeBookedProjection.of("1", "SEPTEMBER", 20),
                OfficeBookedProjection.of("2", "SEPTEMBER", 15),
                OfficeBookedProjection.of("3", "SEPTEMBER", 5)
        );
    }
}

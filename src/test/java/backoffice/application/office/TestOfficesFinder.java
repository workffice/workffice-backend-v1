package backoffice.application.office;

import backoffice.application.dto.office.OfficeResponse;
import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.office.OfficeRepository;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.factories.OfficeBranchBuilder;
import backoffice.factories.OfficeBuilder;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Either;
import io.vavr.control.Option;
import shared.application.UseCaseError;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestOfficesFinder {
    OfficeRepository officeRepo = mock(OfficeRepository.class);
    OfficeBranchFinder officeBranchFinder = mock(OfficeBranchFinder.class);

    OfficesFinder finder = new OfficesFinder(officeRepo, officeBranchFinder);

    @Test
    void itShouldReturnOfficeBranchNotFoundWhenOfficeBranchDoesNotExist() {
        var officeBranchId = new OfficeBranchId();
        when(officeBranchFinder.find(officeBranchId)).thenReturn(Option.none());

        Either<UseCaseError, List<OfficeResponse>> response = finder.find(officeBranchId);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST);
    }

    @Test
    void itShouldReturnAllOfficesRelatedWithOfficeBranch() {
        var officeBranch = new OfficeBranchBuilder().build();
        var office1 = new OfficeBuilder().build();
        var office2 = new OfficeBuilder().build();
        when(officeBranchFinder.find(officeBranch.id())).thenReturn(Option.of(officeBranch.toResponse()));
        when(officeRepo.findByOfficeBranch(any())).thenReturn(ImmutableList.of(office1, office2));

        Either<UseCaseError, List<OfficeResponse>> response = finder.find(officeBranch.id());

        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).size().isEqualTo(2);
        assertThat(response.get()).containsExactlyInAnyOrder(office1.toResponse(), office2.toResponse());
    }

    @Test
    void itShouldFilterDeletedOffices() {
        var officeBranch = new OfficeBranchBuilder().build();
        var office1 = new OfficeBuilder().build();
        office1.delete(LocalDate.now(Clock.systemUTC()).minusDays(1));
        var office2 = new OfficeBuilder().build();
        when(officeBranchFinder.find(officeBranch.id())).thenReturn(Option.of(officeBranch.toResponse()));
        when(officeRepo.findByOfficeBranch(any())).thenReturn(ImmutableList.of(office1, office2));

        Either<UseCaseError, List<OfficeResponse>> response = finder.find(officeBranch.id());

        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).size().isEqualTo(1);
        assertThat(response.get()).containsExactlyInAnyOrder(office2.toResponse());
    }
}

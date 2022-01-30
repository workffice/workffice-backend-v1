package review.application;

import com.google.inject.internal.util.ImmutableList;
import review.application.dto.OfficeBranchReviewResponse;
import review.domain.office.Office;
import review.domain.office.OfficeRepository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestOfficeBranchReviewCalculator {
    OfficeRepository officeRepo = mock(OfficeRepository.class);

    OfficeBranchReviewCalculator calculator = new OfficeBranchReviewCalculator(officeRepo);

    @Test
    void itShouldReturnVotesInformationForOfficeBranch() {
        var office1 = Office.create("1", "12", 20, 5);
        var office2 = Office.create("2", "12", 15, 5);
        var office3 = Office.create("3", "12", 10, 5);
        when(officeRepo.findByOfficeBranchId("12")).thenReturn(ImmutableList.of(office1, office2, office3));

        var response = calculator.calculate("12");

        assertThat(response).isEqualTo(OfficeBranchReviewResponse.of("12", 15, (4 + 3 + 2) / 3));
    }
}

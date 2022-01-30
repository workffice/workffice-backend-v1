package search.application;

import com.google.common.collect.ImmutableList;
import io.vavr.Tuple;
import search.application.dto.OfficeBranchResponse;
import search.application.dto.SearchCriteria;
import search.domain.OfficeBranchRepository;
import search.domain.spec.Between;
import search.domain.spec.Equal;
import search.domain.spec.GreaterThan;
import search.domain.spec.LessThan;
import search.domain.spec.Specification;
import search.factories.OfficeBranchBuilder;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestOfficeBranchSearcher {
    OfficeBranchRepository officeBranchRepo = mock(OfficeBranchRepository.class);
    ArgumentCaptor<List<Specification>> specsArgumentCaptor = ArgumentCaptor.forClass(List.class);

    OfficeBranchSearcher searcher = new OfficeBranchSearcher(officeBranchRepo);

    @Test
    void itShouldReturnOfficeBranchesThatHasTheNameSpecified() {
        var officeBranch = OfficeBranchBuilder.builder().withName("Some name").build();
        when(officeBranchRepo.search(any(List.class), eq(0), eq(3)))
                .thenReturn(ImmutableList.of(officeBranch));
        var pageable = PageRequest.of(0, 3);
        var searchCriteria = SearchCriteria.of(
                ImmutableList.of(Tuple.of(SearchCriteria.Field.OFFICE_BRANCH_NAME, "Some name"))
        );

        searcher.search(pageable, searchCriteria);

        verify(officeBranchRepo, times(1)).search(
                specsArgumentCaptor.capture(),
                eq(0),
                eq(3)
        );
        var specs = specsArgumentCaptor.getValue();
        assertThat(specs).size().isEqualTo(1);
        assertThat(specs.get(0).field()).isEqualTo("name");
        assertThat(specs.get(0).value()).isEqualTo("Some name");
    }

    @Test
    void itShouldReturnOfficeBranchesThatContainSharedOffices() {
        var officeBranch = OfficeBranchBuilder.builder().build();
        when(officeBranchRepo.search(any(List.class), eq(0), eq(3)))
                .thenReturn(ImmutableList.of(officeBranch));
        var pageable = PageRequest.of(0, 3);
        var searchCriteria = SearchCriteria.of(
                ImmutableList.of(Tuple.of(SearchCriteria.Field.OFFICE_TYPE, "SHARED"))
        );

        var response = searcher.search(pageable, searchCriteria);

        assertThat(response.getContent()).containsExactly(officeBranch.toResponse());
        verify(officeBranchRepo, times(1)).search(
                specsArgumentCaptor.capture(),
                eq(0),
                eq(3)
        );
        var specs = specsArgumentCaptor.getValue();
        assertThat(specs).size().isEqualTo(1);
        assertThat(specs.get(0).field()).isEqualTo("offices");
        Equal value = (Equal) specs.get(0).value();
        assertThat(value.field()).isEqualTo("privacy");
        assertThat(value.value()).isEqualTo("SHARED");
    }

    @Test
    void itShouldCallSearchRepoWith2Specs() {
        var officeBranch = OfficeBranchBuilder.builder().build();
        when(officeBranchRepo.search(any(List.class), eq(0), eq(3)))
                .thenReturn(ImmutableList.of(officeBranch));
        var pageable = PageRequest.of(0, 3);
        var searchCriteria = SearchCriteria.of(
                ImmutableList.of(
                        Tuple.of(SearchCriteria.Field.OFFICE_TYPE, "SHARED"),
                        Tuple.of(SearchCriteria.Field.OFFICE_BRANCH_NAME, "Monumental")
                )
        );

        var response = searcher.search(pageable, searchCriteria);

        assertThat(response.getContent()).containsExactly(officeBranch.toResponse());
        verify(officeBranchRepo, times(1)).search(
                specsArgumentCaptor.capture(),
                eq(0),
                eq(3)
        );
        var specs = specsArgumentCaptor.getValue();
        assertThat(specs).size().isEqualTo(2);
    }

    @Test
    void itShouldReturnThe3FirstOfficeBranches() {
        var officeBranch = OfficeBranchBuilder.builder().withName("Some name").build();
        var officeBranch2 = OfficeBranchBuilder.builder().withName("Some name").build();
        var officeBranch3 = OfficeBranchBuilder.builder().withName("Some name").build();
        when(officeBranchRepo.search(any(List.class), eq(0), eq(3)))
                .thenReturn(ImmutableList.of(officeBranch, officeBranch2, officeBranch3));
        when(officeBranchRepo.count(any(List.class))).thenReturn(9L);
        var pageable = PageRequest.of(0, 3);
        var searchCriteria = SearchCriteria.of(
                ImmutableList.of(Tuple.of(SearchCriteria.Field.OFFICE_BRANCH_NAME, "Some name"))
        );

        Page<OfficeBranchResponse> response = searcher.search(pageable, searchCriteria);

        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.getTotalElements()).isEqualTo(9);
        assertThat(response.getContent()).containsExactlyInAnyOrder(
                officeBranch.toResponse(),
                officeBranch2.toResponse(),
                officeBranch3.toResponse()
        );
    }

    @Test
    void itShouldReturnEmptyPageWhenPass2SpecsForOfficeCapacity() {
        /* Clients can only specify one criteria for office capacity GT, LT or BETWEEN */
        var pageable = PageRequest.of(0, 3);
        var searchCriteria = SearchCriteria.of(
                ImmutableList.of(
                        Tuple.of(SearchCriteria.Field.OFFICE_CAPACITY_GT, "1"),
                        Tuple.of(SearchCriteria.Field.OFFICE_CAPACITY_LT, "10")
                )
        );
        var searchCriteria2 = SearchCriteria.of(
                ImmutableList.of(
                        Tuple.of(SearchCriteria.Field.OFFICE_CAPACITY_GT, "1"),
                        Tuple.of(SearchCriteria.Field.OFFICE_CAPACITY_BETWEEN, "1-10")
                )
        );

        var response = searcher.search(pageable, searchCriteria);
        var response2 = searcher.search(pageable, searchCriteria2);

        assertThat(response.getContent()).isEmpty();
        assertThat(response2.getContent()).isEmpty();
    }

    @Test
    void itShouldReturnEmptyPageWhenGreaterThanOrLessThanOfficeCapacityIsNotANumber() {
        var pageable = PageRequest.of(0, 3);
        var invalidCriteria1 = SearchCriteria.of(
                ImmutableList.of(Tuple.of(SearchCriteria.Field.OFFICE_CAPACITY_GT, "asd"))
        );
        var invalidCriteria2 = SearchCriteria.of(
                ImmutableList.of(Tuple.of(SearchCriteria.Field.OFFICE_CAPACITY_LT, "ten"))
        );

        var response = searcher.search(pageable, invalidCriteria1);
        var response2 = searcher.search(pageable, invalidCriteria2);

        assertThat(response.getContent()).isEmpty();
        assertThat(response2.getContent()).isEmpty();
    }


    @ParameterizedTest
    @ValueSource(strings = {"1/10", "1.10", "5&80", "ten-fifty"})
    void itShouldReturnEmptyPageWhenBetweenCapacityHasWrongFormat(String invalidArgument) {
        /* Right format for between is 1-10 */
        var pageable = PageRequest.of(0, 3);
        var invalidCriteria = SearchCriteria.of(
                ImmutableList.of(Tuple.of(SearchCriteria.Field.OFFICE_CAPACITY_BETWEEN, invalidArgument))
        );

        var response = searcher.search(pageable, invalidCriteria);

        assertThat(response.getContent()).isEmpty();
    }

    @Test
    void itShouldUseGreaterThanSpecification() {
        var officeBranch = OfficeBranchBuilder.builder().build();
        when(officeBranchRepo.search(any(List.class), eq(0), eq(3)))
                .thenReturn(ImmutableList.of(officeBranch));
        var pageable = PageRequest.of(0, 3);
        var searchCriteria = SearchCriteria.of(
                ImmutableList.of(Tuple.of(SearchCriteria.Field.OFFICE_CAPACITY_GT, "10"))
        );

        var response = searcher.search(pageable, searchCriteria);

        assertThat(response.getContent()).containsExactly(officeBranch.toResponse());
        verify(officeBranchRepo, times(1)).search(
                specsArgumentCaptor.capture(),
                eq(0),
                eq(3)
        );
        var specs = specsArgumentCaptor.getValue();
        assertThat(specs.size()).isEqualTo(1);
        assertThat(specs.get(0).field()).isEqualTo("offices"); // First spec is "contains"
        GreaterThan greaterThan = (GreaterThan) specs.get(0).value(); // Second spec is "greaterThan"
        assertThat(greaterThan.field()).isEqualTo("capacity");
        assertThat(greaterThan.value()).isEqualTo(10);
    }

    @Test
    void itShouldUseLessThanSpecification() {
        var officeBranch = OfficeBranchBuilder.builder().build();
        when(officeBranchRepo.search(any(List.class), eq(0), eq(3)))
                .thenReturn(ImmutableList.of(officeBranch));
        var pageable = PageRequest.of(0, 3);
        var searchCriteria = SearchCriteria.of(
                ImmutableList.of(Tuple.of(SearchCriteria.Field.OFFICE_CAPACITY_LT, "50"))
        );

        var response = searcher.search(pageable, searchCriteria);

        assertThat(response.getContent()).containsExactly(officeBranch.toResponse());
        verify(officeBranchRepo, times(1)).search(
                specsArgumentCaptor.capture(),
                eq(0),
                eq(3)
        );
        var specs = specsArgumentCaptor.getValue();
        assertThat(specs.size()).isEqualTo(1);
        assertThat(specs.get(0).field()).isEqualTo("offices"); // First spec is "contains"
        LessThan lessThan = (LessThan) specs.get(0).value(); // Second spec is "lessThan"
        assertThat(lessThan.field()).isEqualTo("capacity");
        assertThat(lessThan.value()).isEqualTo(50);
    }

    @Test
    void itShouldParseBetweenParameters() {
        var officeBranch = OfficeBranchBuilder.builder().build();
        when(officeBranchRepo.search(any(List.class), eq(0), eq(3)))
                .thenReturn(ImmutableList.of(officeBranch));
        var pageable = PageRequest.of(0, 3);
        var searchCriteria = SearchCriteria.of(
                ImmutableList.of(Tuple.of(SearchCriteria.Field.OFFICE_CAPACITY_BETWEEN, "10-50"))
        );

        var response = searcher.search(pageable, searchCriteria);

        assertThat(response.getContent()).containsExactly(officeBranch.toResponse());
        verify(officeBranchRepo, times(1)).search(
                specsArgumentCaptor.capture(),
                eq(0),
                eq(3)
        );
        var specs = specsArgumentCaptor.getValue();
        assertThat(specs.size()).isEqualTo(1);
        assertThat(specs.get(0).field()).isEqualTo("offices"); // First spec is "contains"
        Between between = (Between) specs.get(0).value(); // Second spec is "between"
        assertThat(between.field()).isEqualTo("capacity");
        assertThat(between.value()).isEqualTo(Tuple.of(10, 50));
    }
}

package search.application;

import com.google.common.collect.ImmutableList;
import io.vavr.Tuple2;
import search.application.dto.OfficeBranchResponse;
import search.application.dto.SearchCriteria;
import search.domain.OfficeBranch;
import search.domain.OfficeBranchRepository;
import search.domain.spec.Specification;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

@Service
public class OfficeBranchSearcher {
    private final OfficeBranchRepository officeBranchRepo;

    public OfficeBranchSearcher(OfficeBranchRepository officeBranchRepo) {
        this.officeBranchRepo = officeBranchRepo;
    }

    private Specification obtainSpecification(SearchCriteria.Field field, String value) {
        return Match(field).of(
                Case($(SearchCriteria.Field.OFFICE_BRANCH_NAME), () -> Specification.eq("name", value)),
                Case($(SearchCriteria.Field.OFFICE_TYPE), () ->
                        Specification.anyMatch("offices", Specification.eq("privacy", value))),
                Case($(SearchCriteria.Field.OFFICE_CAPACITY_GT), () ->
                        Specification.anyMatch("offices", Specification.gt("capacity", Integer.valueOf(value)))),
                Case($(SearchCriteria.Field.OFFICE_CAPACITY_LT), () ->
                        Specification.anyMatch("offices", Specification.lt("capacity", Integer.valueOf(value)))),
                Case($(SearchCriteria.Field.OFFICE_CAPACITY_BETWEEN), () -> Specification.anyMatch(
                        "offices",
                        Specification.between(
                                "capacity",
                                Integer.valueOf(value.split("-")[0]),
                                Integer.valueOf(value.split("-")[1])
                        )
                ))
        );
    }

    private boolean conditionsAreValid(List<Tuple2<SearchCriteria.Field, String>> conditions) {
        var capacityFields = conditions.stream()
                .map(Tuple2::_1)
                .filter(field -> ImmutableList.of(
                        SearchCriteria.Field.OFFICE_CAPACITY_GT,
                        SearchCriteria.Field.OFFICE_CAPACITY_LT,
                        SearchCriteria.Field.OFFICE_CAPACITY_BETWEEN
                ).contains(field))
                .collect(Collectors.toSet());
        var capacityValuesAreIntegers = conditions.stream()
                .filter(condition -> ImmutableList.of(
                        SearchCriteria.Field.OFFICE_CAPACITY_GT,
                        SearchCriteria.Field.OFFICE_CAPACITY_LT).contains(condition._1))
                .allMatch(condition -> condition._2.matches("[0-9]+"));
        var officeCapacityBetweenCriteriaHasRightFormat = conditions
                .stream()
                .filter(condition -> condition._1.equals(SearchCriteria.Field.OFFICE_CAPACITY_BETWEEN))
                .allMatch(condition -> condition._2.matches("[0-9]+-[0-9]+"));

        if (capacityFields.isEmpty())
            return true;
        return capacityFields.size() == 1
                && capacityValuesAreIntegers
                && officeCapacityBetweenCriteriaHasRightFormat;
    }

    public Page<OfficeBranchResponse> search(Pageable pageable, SearchCriteria searchCriteria) {
        if (!conditionsAreValid(searchCriteria.getConditions()))
            return Page.empty();
        var specs = searchCriteria.getConditions()
                .stream()
                .map(condition -> condition.apply(this::obtainSpecification))
                .collect(Collectors.toList());
        var list = officeBranchRepo
                .search(specs, (int) pageable.getOffset(), pageable.getPageSize())
                .stream()
                .map(OfficeBranch::toResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(list, pageable, officeBranchRepo.count(specs));
    }

}

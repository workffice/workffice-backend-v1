package controller;

import controller.response.PaginatedResponse;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import search.application.OfficeBranchSearcher;
import search.application.dto.OfficeBranchResponse;
import search.application.dto.SearchCriteria;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/office_branches/search")
public class OfficeBranchSearchController extends BaseController {

    @Autowired
    private OfficeBranchSearcher officeBranchSearcher;

    List<Tuple2<SearchCriteria.Field, String>> obtainCriterias(
            Optional<String> name,
            Optional<String> officeType,
            Optional<Integer> officeCapacityGT,
            Optional<Integer> officeCapacityLT
    ) {
        List<Tuple2<SearchCriteria.Field, Optional<String>>> criterias = new ArrayList<>() {{
            add(Tuple.of(SearchCriteria.Field.OFFICE_BRANCH_NAME, name));
            add(Tuple.of(SearchCriteria.Field.OFFICE_TYPE, officeType));
        }};
        if (officeCapacityGT.isPresent() && officeCapacityLT.isPresent())
            criterias.add(Tuple.of(
                    SearchCriteria.Field.OFFICE_CAPACITY_BETWEEN,
                    officeCapacityGT.map(value -> value + "-" + officeCapacityLT.get())
            ));
        else {
            criterias.add(Tuple.of(SearchCriteria.Field.OFFICE_CAPACITY_GT, officeCapacityGT.map(String::valueOf)));
            criterias.add(Tuple.of(SearchCriteria.Field.OFFICE_CAPACITY_LT, officeCapacityLT.map(String::valueOf)));
        }
        return criterias.stream()
                .filter(criteria -> criteria._2.isPresent())
                .map(criteria -> criteria.map2(Optional::get))
                .collect(Collectors.toList());
    }

    @GetMapping("/")
    public ResponseEntity<?> search(
            Pageable pageable,
            @RequestParam Optional<String> name,
            @RequestParam(name = "office_type") Optional<String> officeType,
            @RequestParam(name = "office_capacity_gt") Optional<Integer> officeCapacityGT,
            @RequestParam(name = "office_capacity_lt") Optional<Integer> officeCapacityLT
    ) {
        var criterias = obtainCriterias(name, officeType, officeCapacityGT, officeCapacityLT);
        var searchCriteria = SearchCriteria.of(criterias);
        Page<OfficeBranchResponse> officeBranchesPaged = officeBranchSearcher.search(pageable, searchCriteria);
        var response = new PaginatedResponse<>(
                officeBranchesPaged.getContent(),
                officeBranchesPaged.getSize(),
                officeBranchesPaged.isLast(),
                officeBranchesPaged.getTotalPages(),
                officeBranchesPaged.getTotalPages() == 0 ? 0 : officeBranchesPaged.getNumber() + 1
        );
        return ResponseEntity.ok(response);
    }
}

package search.application.dto;

import io.vavr.Tuple2;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@Getter
public class SearchCriteria {
    private List<Tuple2<Field, String>> conditions;

    public enum Field {
        OFFICE_BRANCH_NAME,
        OFFICE_TYPE,
        OFFICE_CAPACITY_GT,
        OFFICE_CAPACITY_LT,
        OFFICE_CAPACITY_BETWEEN,
    }
}

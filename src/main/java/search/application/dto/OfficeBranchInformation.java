package search.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor(staticName = "of")
@Getter
public class OfficeBranchInformation {
    private final String       name;
    private final String       province;
    private final String       city;
    private final String       street;
    private final String       phone;
    private final List<String> images;
}

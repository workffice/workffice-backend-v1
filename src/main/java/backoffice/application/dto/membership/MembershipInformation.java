package backoffice.application.dto.membership;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@AllArgsConstructor(staticName = "of")
@Getter
public class MembershipInformation {
    private final String name, description;
    private final Integer pricePerMonth;
    private final Set<String> accessDays;
}

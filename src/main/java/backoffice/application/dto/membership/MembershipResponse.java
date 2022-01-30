package backoffice.application.dto.membership;

import lombok.Value;

import java.util.Set;

@Value(staticConstructor = "of")
public class MembershipResponse {
    String id;
    String name;
    String description;
    Set<String> accessDays;
    Integer pricePerMonth;
}

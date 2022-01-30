package booking.application.dto.membership_acquisition;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor(staticName = "of")
@Getter
@EqualsAndHashCode
public class MembershipAcquisitionPreference {
    private final String membershipAcquisitionId;
    private final String membershipName;
    private final String buyerEmail;
    private final Float  price;
    private final String description;
}

package report.domain;

import lombok.Value;

@Value(staticConstructor = "of")
public class OfficeBookedProjection {
    String  officeId;
    String  month;
    Integer totalBookings;
}

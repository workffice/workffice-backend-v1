package backoffice.application.dto.office_inactivity;

import lombok.Value;

import java.time.LocalDate;

@Value(staticConstructor = "of")
public class InactivityResponse {
    String id;
    String type;
    String dayOfWeek;
    LocalDate specificInactivityDay;
}

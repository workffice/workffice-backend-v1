package backoffice.application.dto.office_inactivity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalDate;

@AllArgsConstructor(staticName = "of")
@Getter
public class InactivityInformation {
    String type;
    DayOfWeek dayOfWeek;
    LocalDate specificInactivityDay;
}

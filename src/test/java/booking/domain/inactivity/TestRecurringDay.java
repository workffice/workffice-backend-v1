package booking.domain.inactivity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRecurringDay {

    @Test
    void itShouldReturnFalseWhenDayOfDateIsNotEqualToRecurringDay() {
        var recurringDay = new RecurringDay(new InactivityId(), DayOfWeek.THURSDAY);

        // 15th september of 2021 was a wednesday
        var date = LocalDate.of(2021, 9, 15);
        assertThat(recurringDay.isUnavailableAt(date)).isFalse();
    }

    @Test
    void itShouldReturnTrueWhenDayOfDateIsEqualToRecurringDay() {
        var recurringDay = new RecurringDay(new InactivityId(), DayOfWeek.THURSDAY);

        // 16th september of 2021 was a thursday
        var date = LocalDate.of(2021, 9, 16);
        assertThat(recurringDay.isUnavailableAt(date)).isTrue();
    }
}

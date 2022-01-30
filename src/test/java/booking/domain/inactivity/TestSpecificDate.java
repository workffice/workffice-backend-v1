package booking.domain.inactivity;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSpecificDate {

    @Test
    void itShouldReturnFalseWhenDateSpecifiedIsNotEqualToDateUnavailable() {
        var specificDateInactivity = new SpecificDate(new InactivityId(), LocalDate.of(2018, 12, 8));

        var date = LocalDate.of(2021, 9, 11);
        assertThat(specificDateInactivity.isUnavailableAt(date)).isFalse();
    }

    @Test
    void itShouldReturnTrueWhenDateSpecifiedIsEqualToDateUnavailable() {
        var specificDateInactivity = new SpecificDate(new InactivityId(), LocalDate.of(2028, 12, 8));

        var date = LocalDate.of(2028, 12, 8);
        assertThat(specificDateInactivity.isUnavailableAt(date)).isTrue();
    }
}

package backoffice.domain;

import backoffice.domain.office_inactivity.Inactivity;
import backoffice.domain.office_inactivity.InactivityId;
import backoffice.domain.office_inactivity.InactivityType;
import backoffice.factories.OfficeBuilder;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.time.DayOfWeek;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestInactivity {
    
    @Test
    void itShouldReturnErrorWhenInactivityTypeIsSpecificDateAndNoSpecificDateIsSpecified() {
        Try<Inactivity> inactivity = Inactivity.create(
                new InactivityId(),
                InactivityType.SPECIFIC_DATE,
                Option.of(DayOfWeek.MONDAY),
                Option.none(),
                new OfficeBuilder().build()
        );
        
        assertThat(inactivity.isFailure()).isTrue();
    }
    
    @Test
    void itShouldReturnErrorWhenInactivityTypeIsRecurringAndNoRecurringDayIsSpecified() {
        Try<Inactivity> inactivity = Inactivity.create(
                new InactivityId(),
                InactivityType.RECURRING_DAY,
                Option.none(),
                Option.of(LocalDate.now()),
                new OfficeBuilder().build()
        );
        
        assertThat(inactivity.isFailure()).isTrue();
    }
    
    @Test
    void itShouldCreateInactivityWhenDataIsConsistent() {
        Try<Inactivity> specificDateInactivity = Inactivity.create(
                new InactivityId(),
                InactivityType.SPECIFIC_DATE,
                Option.none(),
                Option.of(LocalDate.now()),
                new OfficeBuilder().build()
        );
        
        Try<Inactivity> recurringDayInactivity = Inactivity.create(
                new InactivityId(),
                InactivityType.RECURRING_DAY,
                Option.of(DayOfWeek.MONDAY),
                Option.none(),
                new OfficeBuilder().build()
        );
        
        assertThat(specificDateInactivity.isFailure()).isFalse();
        assertThat(specificDateInactivity.get().specificInactivityDay().isDefined()).isTrue();
        assertThat(specificDateInactivity.get().dayOfWeek().isEmpty()).isTrue();
        
        assertThat(recurringDayInactivity.isFailure()).isFalse();
        assertThat(recurringDayInactivity.get().specificInactivityDay().isEmpty()).isTrue();
        assertThat(recurringDayInactivity.get().dayOfWeek().isDefined()).isTrue();
    }
}

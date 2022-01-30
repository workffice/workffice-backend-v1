package booking.infrastructure.repositories;

import booking.domain.inactivity.Inactivity;
import booking.domain.inactivity.InactivityId;
import booking.domain.inactivity.RecurringDay;
import booking.domain.inactivity.SpecificDate;
import io.vavr.control.Option;
import server.WorkfficeApplication;

import java.time.DayOfWeek;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = WorkfficeApplication.class)
public class TestInactivityMySQLRepository {
    @Autowired
    InactivityMySQLRepository inactivityRepo;

    @Test
    void itShouldDeleteInactivity() {
        var inactivity = new RecurringDay(new InactivityId(), DayOfWeek.MONDAY);
        var inactivity2 = new SpecificDate(new InactivityId(), LocalDate.of(2018, 12, 8));
        inactivityRepo.store(inactivity);
        inactivityRepo.store(inactivity2);

        Option<Inactivity> inactivityFound = inactivityRepo.findById(inactivity.id());
        Option<Inactivity> inactivityFound2 = inactivityRepo.findById(inactivity2.id());
        assertThat(inactivityFound.isDefined()).isTrue();
        assertThat(inactivityFound2.isDefined()).isTrue();

        inactivityRepo.delete(inactivity.id());
        inactivityRepo.delete(inactivity2.id());
        inactivityFound = inactivityRepo.findById(inactivity.id());
        inactivityFound2 = inactivityRepo.findById(inactivity2.id());
        assertThat(inactivityFound.isDefined()).isFalse();
        assertThat(inactivityFound2.isDefined()).isFalse();
    }
}

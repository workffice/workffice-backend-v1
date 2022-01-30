package booking.infrastructure.repositories;

import booking.domain.inactivity.InactivityId;
import booking.domain.inactivity.RecurringDay;
import booking.domain.office.Office;
import booking.domain.office.OfficeId;
import booking.domain.office.privacy.PrivateOffice;
import booking.domain.office.privacy.SharedOffice;
import io.vavr.control.Try;
import server.WorkfficeApplication;

import java.time.DayOfWeek;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = WorkfficeApplication.class)
public class TestOfficeMySQLRepository {
    @Autowired
    OfficeMySQLRepository officeRepo;

    @Test
    void itShouldStoreOfficeWithPrivacy() {
        var officeId = new OfficeId();
        var office = Office.create(officeId, "123", "NAME", 100, new PrivateOffice(10));

        Try<Void> response = officeRepo.store(office);

        assertThat(response.isSuccess()).isTrue();
        var officeStored = officeRepo.findById(officeId).get();
        assertThat(officeStored.id()).isEqualTo(officeId);
    }

    @Test
    void itShouldUpdateOffice() {
        var officeId = new OfficeId();
        var office = Office.create(officeId, "123", "NAME", 100, new PrivateOffice(10));
        officeRepo.store(office).get();

        office.update("New Name", 50, new SharedOffice(1, 10));
        office.addInactivity(new RecurringDay(new InactivityId(), DayOfWeek.FRIDAY));
        Try<Void> response = officeRepo.update(office);

        response.get();
        assertThat(response.isSuccess()).isTrue();
        var officeUpdated = officeRepo.findById(officeId).get();
        assertThat(officeUpdated.name()).isEqualTo("New Name");
        assertThat(officeUpdated.price()).isEqualTo(50);
        assertThat(officeUpdated.inactivities()).size().isEqualTo(1);
    }
}

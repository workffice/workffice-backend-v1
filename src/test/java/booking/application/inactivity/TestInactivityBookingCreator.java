package booking.application.inactivity;

import backoffice.domain.office_inactivity.InactivityCreatedEvent;
import booking.domain.inactivity.InactivityId;
import booking.domain.inactivity.RecurringDay;
import booking.domain.inactivity.SpecificDate;
import booking.domain.office.Office;
import booking.domain.office.OfficeId;
import booking.domain.office.OfficeRepository;
import booking.factories.OfficeBuilder;
import io.vavr.control.Option;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestInactivityBookingCreator {
    OfficeRepository officeRepo = mock(OfficeRepository.class);
    ArgumentCaptor<Office> officeArgumentCaptor = ArgumentCaptor.forClass(Office.class);

    InactivityBookingCreator creator = new InactivityBookingCreator(officeRepo);

    @Test
    void itShouldNotCallOfficeUpdateWhenItDoesNotExist() {
        var officeId = new OfficeId();
        var event = InactivityCreatedEvent.of(
                UUID.randomUUID().toString(),
                officeId.toString(),
                "RECURRING_DAY",
                Option.of(DayOfWeek.MONDAY),
                Option.none()
        );
        when(officeRepo.findById(officeId)).thenReturn(Option.none());

        creator.create(event);

        verify(officeRepo, times(0)).update(any());
    }

    @Test
    void itShouldCallOfficeUpdateWithRecurringDayInactivity() {
        var office = new OfficeBuilder().build();
        var inactivityId = new InactivityId();
        var event = InactivityCreatedEvent.of(
                inactivityId.toString(),
                office.id().toString(),
                "RECURRING_DAY",
                Option.of(DayOfWeek.MONDAY),
                Option.none()
        );
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));

        creator.create(event);

        verify(officeRepo, times(1)).update(officeArgumentCaptor.capture());
        var officeUpdated = officeArgumentCaptor.getValue();
        assertThat(officeUpdated.inactivities()).containsExactly(new RecurringDay(inactivityId, DayOfWeek.MONDAY));
    }

    @Test
    void itShouldCallOfficeUpdateWithSpecificDateInactivity() {
        var office = new OfficeBuilder().build();
        var inactivityId = new InactivityId();
        var event = InactivityCreatedEvent.of(
                inactivityId.toString(),
                office.id().toString(),
                "SPECIFIC_DATE",
                Option.none(),
                Option.of(LocalDate.of(2018, 12, 8))
        );
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));

        creator.create(event);

        verify(officeRepo, times(1)).update(officeArgumentCaptor.capture());
        var officeUpdated = officeArgumentCaptor.getValue();
        assertThat(officeUpdated.inactivities())
                .containsExactly(new SpecificDate(inactivityId, LocalDate.of(2018, 12, 8)));
    }
}

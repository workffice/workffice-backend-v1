package booking.application.office;

import backoffice.domain.office.OfficeDeletedEvent;
import booking.domain.office.Office;
import booking.domain.office.OfficeId;
import booking.domain.office.OfficeRepository;
import booking.factories.OfficeBuilder;
import io.vavr.control.Option;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestOfficeBookingDeleter {
    OfficeRepository officeRepo = mock(OfficeRepository.class);
    ArgumentCaptor<Office> officeArgumentCaptor = ArgumentCaptor.forClass(Office.class);

    OfficeBookingDeleter deleter = new OfficeBookingDeleter(officeRepo);

    @Test
    void itShouldNoDeleteOfficeWhenItDoesNotExist() {
        var officeId = new OfficeId();
        when(officeRepo.findById(officeId)).thenReturn(Option.none());
        var event = OfficeDeletedEvent.of("1", officeId.toString());

        deleter.delete(event);

        verify(officeRepo, times(0)).update(any());
    }

    @Test
    void itShouldDeleteOffice() {
        var office = new OfficeBuilder().build();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        var event = OfficeDeletedEvent.of("1", office.id().toString());

        deleter.delete(event);

        verify(officeRepo, times(1)).update(officeArgumentCaptor.capture());
        var officeDeleted = officeArgumentCaptor.getValue();
        assertThat(officeDeleted.isDeleted()).isTrue();
    }
}

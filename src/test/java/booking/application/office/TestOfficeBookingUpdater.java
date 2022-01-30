package booking.application.office;

import backoffice.domain.office.OfficeUpdatedEvent;
import booking.domain.office.Office;
import booking.domain.office.OfficeId;
import booking.domain.office.OfficeRepository;
import booking.factories.OfficeBuilder;
import io.vavr.control.Option;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestOfficeBookingUpdater {
    OfficeRepository officeRepo = mock(OfficeRepository.class);
    ArgumentCaptor<Office> officeArgumentCaptor = ArgumentCaptor.forClass(Office.class);

    OfficeBookingUpdater updater = new OfficeBookingUpdater(officeRepo);

    @Test
    void itShouldNotUpdateOfficeWhenThereIsNoOfficeWithIdProvided() {
        var officeId = UUID.randomUUID().toString();
        var event = OfficeUpdatedEvent.of(
                officeId,
                "12",
                "New awesome name",
                "INVALID_PRIVACY_TYPE",
                150,
                20,
                2,
                10
        );
        when(officeRepo.findById(OfficeId.fromString(officeId))).thenReturn(Option.none());

        updater.update(event);

        verify(officeRepo, times(0)).update(any());
    }

    @Test
    void itShouldNotUpdateOfficeWhenPrivacyTypeProvidedIsNotValid() {
        var office = new OfficeBuilder().build();
        var event = OfficeUpdatedEvent.of(
                office.id().toString(),
                "12",
                "New awesome name",
                "INVALID_PRIVACY_TYPE",
                150,
                20,
                2,
                10
        );
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));

        updater.update(event);

        verify(officeRepo, times(0)).update(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PRIVATE", "SHARED"})
    void itShouldUpdateOfficeWithInfoSpecifiedInEvent(String privacyType) {
        var office = new OfficeBuilder().build();
        var event = OfficeUpdatedEvent.of(
                office.id().toString(),
                "12",
                "New awesome name",
                privacyType,
                150,
                20,
                2,
                10
        );
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));

        updater.update(event);

        verify(officeRepo, times(1)).update(officeArgumentCaptor.capture());
        var officeUpdated = officeArgumentCaptor.getValue();
        assertThat(officeUpdated.name()).isEqualTo("New awesome name");
        assertThat(officeUpdated.price()).isEqualTo(150);
    }
}

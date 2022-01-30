package booking.application.office;

import backoffice.domain.office.OfficeCreatedEvent;
import booking.domain.office.Office;
import booking.domain.office.OfficeId;
import booking.domain.office.OfficeRepository;

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

public class TestOfficeBookingCreator {
    OfficeRepository officeRepo = mock(OfficeRepository.class);
    ArgumentCaptor<Office> officeArgumentCaptor = ArgumentCaptor.forClass(Office.class);

    OfficeBookingCreator creator = new OfficeBookingCreator(officeRepo);

    @ParameterizedTest
    @ValueSource(strings = {"PRIVATE", "SHARED"})
    void itShouldCreateOfficeWithInfoSpecified(String privacyType) {
        var officeId = UUID.randomUUID().toString();
        var event = OfficeCreatedEvent.of(
                officeId,
                "10",
                "Some name",
                privacyType,
                100,
                10,
                1,
                10
        );

        creator.create(event);

        verify(officeRepo, times(1)).store(officeArgumentCaptor.capture());
        var officeStored = officeArgumentCaptor.getValue();
        assertThat(officeStored.id()).isEqualTo(OfficeId.fromString(officeId));
        assertThat(officeStored.name()).isEqualTo("Some name");
        assertThat(officeStored.price()).isEqualTo(100);
    }

    @Test
    void itShouldNotStoreOfficeWhenPrivateTypeIsInvalid() {
        var officeId = UUID.randomUUID().toString();
        var event = OfficeCreatedEvent.of(
                officeId,
                "10",
                "Some name",
                "INVALID_PRIVACY_TYPE",
                100,
                10,
                1,
                10
        );

        creator.create(event);

        verify(officeRepo, times(0)).store(any());
    }
}

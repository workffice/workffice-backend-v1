package backoffice.application.office_inactivity;

import backoffice.application.dto.office.OfficeError;
import backoffice.application.dto.office_inactivity.InactivityResponse;
import backoffice.domain.office.OfficeId;
import backoffice.domain.office.OfficeRepository;
import backoffice.domain.office_inactivity.InactivityRepository;
import backoffice.factories.InactivityBuilder;
import backoffice.factories.OfficeBuilder;
import io.vavr.control.Either;
import io.vavr.control.Option;
import shared.application.UseCaseError;

import java.time.DayOfWeek;
import java.util.List;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestInactivitiesFinder {
    InactivityRepository inactivityRepo = mock(InactivityRepository.class);
    OfficeRepository officeRepo = mock(OfficeRepository.class);

    InactivitiesFinder finder = new InactivitiesFinder(inactivityRepo, officeRepo);

    @Test
    void itShouldReturnOfficeNotFoundWhenOfficeDoesNotExist() {
        var officeId = new OfficeId();
        when(officeRepo.findById(officeId)).thenReturn(Option.none());

        Either<UseCaseError, List<InactivityResponse>> response = finder.find(officeId);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeError.OFFICE_NOT_FOUND);
    }

    @Test
    void itShouldReturnAllInactivitiesRelatedWithOffice() {
        var office = new OfficeBuilder().build();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        var inactivity1 = new InactivityBuilder().build();
        var inactivity2 = new InactivityBuilder().build();
        var inactivity3 = new InactivityBuilder().build();
        when(inactivityRepo.findAllByOffice(any()))
                .thenReturn(Lists.newArrayList(inactivity1, inactivity2, inactivity3));

        Either<UseCaseError, List<InactivityResponse>> response = finder.find(office.id());

        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).size().isEqualTo(3);
        assertThat(response.get()).containsExactlyInAnyOrder(
                InactivityResponse.of(
                        inactivity1.id().toString(),
                        inactivity1.type().toString(),
                        inactivity1.dayOfWeek().map(DayOfWeek::toString).getOrNull(),
                        inactivity1.specificInactivityDay().getOrNull()
                ), InactivityResponse.of(
                        inactivity2.id().toString(),
                        inactivity2.type().toString(),
                        inactivity2.dayOfWeek().map(DayOfWeek::toString).getOrNull(),
                        inactivity2.specificInactivityDay().getOrNull()
                ), InactivityResponse.of(
                        inactivity3.id().toString(),
                        inactivity3.type().toString(),
                        inactivity3.dayOfWeek().map(DayOfWeek::toString).getOrNull(),
                        inactivity3.specificInactivityDay().getOrNull()
                )
        );
    }
}

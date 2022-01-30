package backoffice.application.office;

import backoffice.application.dto.office.OfficeError;
import backoffice.application.dto.office.OfficeResponse;
import backoffice.domain.office.OfficeId;
import backoffice.domain.office.OfficeRepository;
import backoffice.factories.OfficeBuilder;
import io.vavr.control.Either;
import io.vavr.control.Option;
import shared.application.UseCaseError;

import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestOfficeFinder {
    OfficeRepository officeRepo = mock(OfficeRepository.class);

    OfficeFinder finder = new OfficeFinder(officeRepo);

    @Test
    void itShouldReturnOfficeNotFoundWhenThereIsNoOfficeWithIdSpecified() {
        var officeId = new OfficeId();
        when(officeRepo.findById(officeId)).thenReturn(Option.none());

        Either<UseCaseError, OfficeResponse> response = finder.find(officeId);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeError.OFFICE_NOT_FOUND);
    }

    @Test
    void itShouldReturnOfficeNotFoundWhenOfficeIsDeleted() {
        var office = new OfficeBuilder().build();
        office.delete(LocalDate.now(Clock.systemUTC()).minusDays(1));
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));

        Either<UseCaseError, OfficeResponse> response = finder.find(office.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeError.OFFICE_NOT_FOUND);
    }


    @Test
    void itShouldReturnOfficeRelatedWithSpecifiedId() {
        var office = new OfficeBuilder().build();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));

        Either<UseCaseError, OfficeResponse> response = finder.find(office.id());

        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).isEqualTo(office.toResponse());
    }
}

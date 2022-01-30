package backoffice.application.office;

import backoffice.application.dto.office.OfficeError;
import backoffice.application.dto.office.OfficeResponse;
import backoffice.domain.office.Office;
import backoffice.domain.office.OfficeId;
import backoffice.domain.office.OfficeRepository;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class OfficeFinder {
    private final OfficeRepository officeRepo;

    public OfficeFinder(OfficeRepository officeRepo) {
        this.officeRepo = officeRepo;
    }

    public Either<UseCaseError, OfficeResponse> find(OfficeId id) {
        return officeRepo
                .findById(id)
                .filter(office -> !office.isDeleted(LocalDate.now(Clock.systemUTC())))
                .toEither((UseCaseError) OfficeError.OFFICE_NOT_FOUND)
                .map(Office::toResponse);
    }
}

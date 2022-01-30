package backoffice.application.office_inactivity;

import backoffice.application.dto.office.OfficeError;
import backoffice.application.dto.office_inactivity.InactivityResponse;
import backoffice.domain.office.OfficeId;
import backoffice.domain.office.OfficeRepository;
import backoffice.domain.office_inactivity.Inactivity;
import backoffice.domain.office_inactivity.InactivityRepository;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import java.time.DayOfWeek;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class InactivitiesFinder {

    private final OfficeRepository     officeRepository;
    private final InactivityRepository inactivityRepo;

    public InactivitiesFinder(InactivityRepository inactivityRepo, OfficeRepository officeRepository) {
        this.inactivityRepo   = inactivityRepo;
        this.officeRepository = officeRepository;
    }

    private List<InactivityResponse> toInactivityResponse(List<Inactivity> inactivities) {
        return inactivities
                .stream()
                .map(inactivity -> InactivityResponse.of(
                        inactivity.id().toString(),
                        inactivity.type().toString(),
                        inactivity.dayOfWeek().map(DayOfWeek::toString).getOrNull(),
                        inactivity.specificInactivityDay().getOrNull()
                )).collect(Collectors.toList());
    }

    public Either<UseCaseError, List<InactivityResponse>> find(OfficeId officeId) {
        return officeRepository
                .findById(officeId)
                .toEither((UseCaseError) OfficeError.OFFICE_NOT_FOUND)
                .map(inactivityRepo::findAllByOffice)
                .map(this::toInactivityResponse);
    }
}

package backoffice.application.office_inactivity;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.office.OfficeError;
import backoffice.application.dto.office_inactivity.InactivityError;
import backoffice.application.dto.office_inactivity.InactivityInformation;
import backoffice.domain.office.Office;
import backoffice.domain.office.OfficeId;
import backoffice.domain.office.OfficeRepository;
import backoffice.domain.office_inactivity.Inactivity;
import backoffice.domain.office_inactivity.InactivityId;
import backoffice.domain.office_inactivity.InactivityRepository;
import backoffice.domain.office_inactivity.InactivityType;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import io.vavr.Tuple;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import shared.application.UseCaseError;
import shared.domain.EventBus;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class InactivitiesUpdater {
    private final InactivityRepository inactivityRepo;
    private final OfficeRepository officeRepo;
    private final PermissionValidator permissionValidator;
    private final EventBus eventBus;

    public InactivitiesUpdater(
            InactivityRepository inactivityRepo,
            OfficeRepository officeRepo,
            PermissionValidator permissionValidator,
            EventBus eventBus
    ) {
        this.inactivityRepo = inactivityRepo;
        this.officeRepo = officeRepo;
        this.permissionValidator = permissionValidator;
        this.eventBus = eventBus;
    }

    private Try<Inactivity> createInactivity(
            InactivityInformation info,
            Office office
    ) {
        return Inactivity.create(
                new InactivityId(),
                InactivityType.valueOf(info.getType()),
                Option.of(info.getDayOfWeek()),
                Option.of(info.getSpecificInactivityDay()),
                office
        );
    }

    private List<Try<Inactivity>> createInactivities(
            List<InactivityInformation> infos,
            Office office
    ) {
        return infos.stream().map(info -> createInactivity(info, office)).collect(Collectors.toList());
    }

    public Either<UseCaseError, Void> updateOfficeInactivities(OfficeId id, List<InactivityInformation> infos) {
        var maybeOffice = officeRepo.findById(id);

        return maybeOffice.toEither((UseCaseError) OfficeError.OFFICE_NOT_FOUND)
                .filterOrElse(
                        office -> permissionValidator.userHasPerms(
                                office.officeBranch(),
                                Permission.create(Access.WRITE, Resource.OFFICE)
                        ),
                        office -> OfficeError.OFFICE_FORBIDDEN)
                .map(office -> this.createInactivities(infos, office))
                .filterOrElse(
                        inactivities -> inactivities.stream().allMatch(Try::isSuccess),
                        inactivities -> InactivityError.INACTIVITY_TYPE_MISMATCH_WITH_DATE)
                .map(inactivities -> inactivities.stream().map(Try::get).collect(Collectors.toSet()))
                .map(inactivities -> {
                    var currentInactivities = inactivityRepo.findAllByOffice(maybeOffice.get());
                    var inactivitiesToDelete = currentInactivities
                            .stream()
                            .filter(inactivity -> !inactivities.contains(inactivity))
                            .collect(Collectors.toList());
                    var inactivitiesToCreate = inactivities
                            .stream()
                            .filter(inactivity -> !currentInactivities.contains(inactivity))
                            .collect(Collectors.toList());
                    return Tuple.of(inactivitiesToCreate, inactivitiesToDelete);
                }).flatMap(inactivities -> inactivityRepo.bulkStore(inactivities._1)
                        .onSuccess(v -> inactivityRepo.delete(inactivities._2))
                        .onSuccess(v -> {
                            inactivities._1.forEach(inactivity -> eventBus
                                    .publish(inactivity.inactivityCreatedEvent()));
                            inactivities._2.forEach(inactivity -> eventBus
                                    .publish(inactivity.inactivityDeletedEvent()));
                        }).toEither(InactivityError.DB_ERROR)
                );
    }
}

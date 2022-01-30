package backoffice.application.office;

import backoffice.application.dto.office.OfficeInformation;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.office.Image;
import backoffice.domain.office.Office;
import backoffice.domain.office.OfficeId;
import backoffice.domain.office.OfficeRepository;
import backoffice.domain.office.Privacy;
import backoffice.domain.office.Tables;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import io.vavr.control.Either;
import io.vavr.control.Try;
import shared.application.UseCaseError;
import shared.domain.EventBus;

import org.springframework.stereotype.Service;

import static backoffice.application.dto.office.OfficeError.DB_ERROR;
import static backoffice.application.dto.office.OfficeError.SHARED_OFFICE_WITHOUT_TABLES;

@Service
public class OfficeCreator {

    private final EventBus eventBus;
    private final OfficeRepository officeRepo;
    private final OfficeBranchFinder officeBranchFinder;

    public OfficeCreator(
            EventBus eventBus,
            OfficeRepository officeRepo,
            OfficeBranchFinder officeBranchFinder
    ) {
        this.eventBus = eventBus;
        this.officeRepo = officeRepo;
        this.officeBranchFinder = officeBranchFinder;
    }

    private Try<Office> createNewOffice(OfficeId id, OfficeInformation info, OfficeBranch officeBranch) {
        Privacy privacy = Try
                .of(() -> Privacy.valueOf(info.getPrivacy()))
                .getOrElse(Privacy.SHARED);
        Image image = new Image(info.getImageUrl());
        return Office.create(
                id,
                info.getName(),
                info.getDescription(),
                info.getCapacity(),
                info.getPrice(),
                image,
                privacy,
                officeBranch,
                Tables.create(info.getTablesQuantity(), info.getCapacityPerTable())
        );
    }

    public Either<UseCaseError, Void> create(
            OfficeId officeId,
            OfficeBranchId officeBranchId,
            OfficeInformation info
    ) {
        return officeBranchFinder
                .findWithAuthorization(officeBranchId, Permission.create(Access.WRITE, Resource.OFFICE))
                .map(OfficeBranch::fromDTO)
                .flatMap(officeBranch -> createNewOffice(officeId, info, officeBranch)
                        .toEither(SHARED_OFFICE_WITHOUT_TABLES))
                .flatMap(office -> officeRepo.store(office)
                        .peek(v ->eventBus.publish(office.officeCreatedEvent()))
                        .toEither(DB_ERROR));
    }
}

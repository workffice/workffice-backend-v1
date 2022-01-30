package backoffice.application.office_branch;

import authentication.application.AuthUserValidator;
import backoffice.application.dto.office_branch.OfficeBranchInformation;
import backoffice.domain.office_branch.Image;
import backoffice.domain.office_branch.Location;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.office_branch.OfficeBranchRepository;
import backoffice.domain.office_holder.OfficeHolder;
import backoffice.domain.office_holder.OfficeHolderId;
import backoffice.domain.office_holder.OfficeHolderRepository;
import io.vavr.control.Either;
import shared.application.UseCaseError;
import shared.domain.EventBus;

import java.util.List;
import org.springframework.stereotype.Service;

import static backoffice.application.dto.office_branch.OfficeBranchError.DB_ERROR;
import static backoffice.application.dto.office_holder.OfficeHolderError.OFFICE_HOLDER_FORBIDDEN;
import static backoffice.application.dto.office_holder.OfficeHolderError.OFFICE_HOLDER_NOT_FOUND;
import static java.util.stream.Collectors.toList;

@Service
public class OfficeBranchCreator {

    private final EventBus eventBus;
    private final AuthUserValidator authUserValidator;
    private final OfficeBranchRepository officeBranchRepo;
    private final OfficeHolderRepository officeHolderRepo;

    public OfficeBranchCreator(
            EventBus eventBus,
            AuthUserValidator authUserValidator,
            OfficeBranchRepository officeBranchRepo,
            OfficeHolderRepository officeHolderRepo
    ) {
        this.eventBus = eventBus;
        this.officeBranchRepo = officeBranchRepo;
        this.officeHolderRepo = officeHolderRepo;
        this.authUserValidator = authUserValidator;
    }

    private OfficeBranch createOfficeBranch(
            OfficeBranchId id,
            OfficeBranchInformation info,
            OfficeHolder officeHolder
    ) {
        Location location = new Location(info.getProvince(), info.getCity(), info.getStreet(), info.getZipCode());
        List<Image> images = info.getImagesUrls()
                .stream()
                .map(Image::new)
                .collect(toList());
        OfficeBranch officeBranch = OfficeBranch.createNew(
                id, officeHolder, info.getName(), info.getDescription(), info.getPhone(), location
        );
        officeBranch.addImages(images);
        return officeBranch;
    }

    public Either<UseCaseError, Void> create(
            OfficeHolderId officeHolderId,
            OfficeBranchId id,
            OfficeBranchInformation info
    ) {
        return officeHolderRepo
                .findById(officeHolderId)
                .toEither((UseCaseError) OFFICE_HOLDER_NOT_FOUND)
                .filterOrElse(
                        officeHolder -> authUserValidator.isSameUserAsAuthenticated(officeHolder.email()),
                        officeHolder -> OFFICE_HOLDER_FORBIDDEN
                )
                .map(officeHolder -> createOfficeBranch(id, info, officeHolder))
                .flatMap(officeBranch -> officeBranchRepo
                        .store(officeBranch)
                        .peek(v -> eventBus.publish(officeBranch.officeBranchCreatedEvent()))
                        .toEither(DB_ERROR)
                );
    }
}

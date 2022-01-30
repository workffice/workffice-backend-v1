package backoffice.application.office_holder;

import authentication.application.AuthUserValidator;
import backoffice.application.dto.office_holder.OfficeHolderResponse;
import backoffice.domain.office_holder.OfficeHolderId;
import backoffice.domain.office_holder.OfficeHolderRepository;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import org.springframework.stereotype.Service;

import static backoffice.application.dto.office_holder.OfficeHolderError.OFFICE_HOLDER_FORBIDDEN;
import static backoffice.application.dto.office_holder.OfficeHolderError.OFFICE_HOLDER_NOT_FOUND;

@Service
public class OfficeHolderFinder {
    private final OfficeHolderRepository officeHolderRepo;
    private final AuthUserValidator authUserValidator;
    
    public OfficeHolderFinder(AuthUserValidator authUserValidator, OfficeHolderRepository officeHolderRepo) {
        this.officeHolderRepo = officeHolderRepo;
        this.authUserValidator = authUserValidator;
    }
    
    public Either<UseCaseError, OfficeHolderResponse> find(OfficeHolderId id) {
        return officeHolderRepo.findById(id)
                .toEither((UseCaseError) OFFICE_HOLDER_NOT_FOUND)
                .filterOrElse(
                        officeHolder -> authUserValidator.isSameUserAsAuthenticated(officeHolder.email()),
                        officeHolder -> OFFICE_HOLDER_FORBIDDEN
                )
                .map(officeHolder -> OfficeHolderResponse.of(
                        officeHolder.id().toString(),
                        officeHolder.email()
                ));
    }
}

package backoffice.application.office_branch;

import authentication.application.AuthUserValidator;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_holder.OfficeHolderRepository;

import org.springframework.stereotype.Service;

/**
 * OfficeBranchAuthValidator validates if the current user
 * has access to the given office branch. It returns true
 * if that is the case and false if not.
 */
@Service
public class OfficeBranchAuthValidator {
    
    private final AuthUserValidator authUserValidator;
    private final OfficeHolderRepository officeHolderRepo;
    
    public OfficeBranchAuthValidator(
            AuthUserValidator authUserValidator,
            OfficeHolderRepository officeHolderRepo
    ) {
        this.authUserValidator = authUserValidator;
        this.officeHolderRepo = officeHolderRepo;
    }
    
    public boolean authUserIsOwner(OfficeBranch officeBranch) {
        return officeHolderRepo
                .findByOfficeBranch(officeBranch)
                .map(officeHolder -> authUserValidator.isSameUserAsAuthenticated(officeHolder.email()))
                .getOrElse(false);
    }
}

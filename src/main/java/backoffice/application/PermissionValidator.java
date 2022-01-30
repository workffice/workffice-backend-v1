package backoffice.application;

import authentication.application.AuthUserFinder;
import authentication.application.dto.user.AuthUserResponse;
import backoffice.application.office_branch.OfficeBranchAuthValidator;
import backoffice.domain.collaborator.Collaborator;
import backoffice.domain.collaborator.CollaboratorRepository;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.role.Permission;
import io.vavr.control.Option;

import org.springframework.stereotype.Service;

@Service
public class PermissionValidator {
    private final AuthUserFinder            authUserFinder;
    private final CollaboratorRepository    collaboratorRepo;
    private final OfficeBranchAuthValidator officeBranchAuthValidator;

    public PermissionValidator(
            AuthUserFinder            authUserFinder,
            CollaboratorRepository    collaboratorRepo,
            OfficeBranchAuthValidator officeBranchAuthValidator
    ) {
        this.authUserFinder            = authUserFinder;
        this.collaboratorRepo          = collaboratorRepo;
        this.officeBranchAuthValidator = officeBranchAuthValidator;
    }

    public boolean userHasPerms(OfficeBranch officeBranch, Permission permission) {
        Option<String> maybeAuthUserEmail = authUserFinder
                .findAuthenticatedUser()
                .map(AuthUserResponse::getEmail);
        if (maybeAuthUserEmail.isEmpty())
            return false;
        return collaboratorRepo
                .find(maybeAuthUserEmail.get(), officeBranch)
                .filter(Collaborator::isActive)
                .map(collaborator -> collaborator.hasPermission(permission))
                .getOrElse(() -> officeBranchAuthValidator.authUserIsOwner(officeBranch));

    }
}

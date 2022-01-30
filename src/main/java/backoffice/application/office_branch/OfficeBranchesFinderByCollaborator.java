package backoffice.application.office_branch;

import authentication.application.AuthUserValidator;
import backoffice.application.dto.collaborator.CollaboratorError;
import backoffice.application.dto.office_branch.OfficeBranchResponse;
import backoffice.domain.collaborator.Collaborator;
import backoffice.domain.collaborator.CollaboratorRepository;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchRepository;
import io.vavr.control.Either;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OfficeBranchesFinderByCollaborator {
    private final AuthUserValidator      authUserValidator;
    private final CollaboratorRepository collaboratorRepo;
    private final OfficeBranchRepository officeBranchRepo;

    public OfficeBranchesFinderByCollaborator(
            AuthUserValidator      authUserValidator,
            CollaboratorRepository collaboratorRepo,
            OfficeBranchRepository officeBranchRepo
    ) {
        this.collaboratorRepo  = collaboratorRepo;
        this.authUserValidator = authUserValidator;
        this.officeBranchRepo  = officeBranchRepo;
    }

    public Either<CollaboratorError, List<OfficeBranchResponse>> find(String collaboratorEmail) {
        if (!authUserValidator.isSameUserAsAuthenticated(collaboratorEmail))
            return Either.left(CollaboratorError.FORBIDDEN);
        var collaborators = collaboratorRepo.find(collaboratorEmail);
        var officeBranchIds = collaborators.stream()
                .map(Collaborator::officeBranch)
                .map(OfficeBranch::id)
                .collect(Collectors.toList());
        var officeBranches = officeBranchRepo
                .findByIds(officeBranchIds).stream()
                .map(OfficeBranch::toResponse)
                .collect(Collectors.toList());
        return Either.right(officeBranches);
    }
}

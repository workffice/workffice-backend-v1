package backoffice.application.collaborator;

import backoffice.application.dto.collaborator.CollaboratorError;
import backoffice.application.dto.collaborator.CollaboratorInformation;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.collaborator.Collaborator;
import backoffice.domain.collaborator.CollaboratorId;
import backoffice.domain.collaborator.CollaboratorRepository;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.domain.role.Role;
import backoffice.domain.role.RoleId;
import backoffice.domain.role.RoleRepository;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CollaboratorCreator {

    private final RoleRepository         roleRepo;
    private final OfficeBranchFinder     officeBranchFinder;
    private final InvitationEmailSender  invitationEmailSender;
    private final CollaboratorRepository collaboratorRepo;

    public CollaboratorCreator(
            RoleRepository         roleRepo,
            OfficeBranchFinder     officeBranchFinder,
            CollaboratorRepository collaboratorRepo,
            InvitationEmailSender  invitationEmailSender
    ) {
        this.roleRepo              = roleRepo;
        this.collaboratorRepo      = collaboratorRepo;
        this.officeBranchFinder    = officeBranchFinder;
        this.invitationEmailSender = invitationEmailSender;
    }

    private Tuple2<OfficeBranch, Set<Role>> officeBranchWithRoles(OfficeBranch officeBranch, Set<UUID> requestedRoles) {
        Set<RoleId> roleIds = requestedRoles.stream().map(RoleId::new).collect(Collectors.toSet());
        var roles = roleRepo.findByOfficeBranch(officeBranch)
                .stream()
                .filter(role -> roleIds.contains(role.id()))
                .collect(Collectors.toSet());
        return Tuple.of(officeBranch, roles);
    }

    private Collaborator createNewCollaborator(
            CollaboratorId id,
            String email,
            String name,
            Tuple2<OfficeBranch, Set<Role>> officeBranchWithRoles
    ) {
        return officeBranchWithRoles
                .apply((officeBranch, roles) -> Collaborator.createNew(id, email, name, roles, officeBranch));
    }

    public Either<UseCaseError, Void> create(
            OfficeBranchId officeBranchId,
            CollaboratorId collaboratorId,
            CollaboratorInformation info
    ) {
        return officeBranchFinder
                .findWithAuthorization(officeBranchId, Permission.create(Access.WRITE, Resource.COLLABORATOR))
                .map(OfficeBranch::fromDTO)
                .filterOrElse(
                        officeBranch -> !collaboratorRepo.exists(info.getEmail(), officeBranch),
                        officeBranch -> CollaboratorError.COLLABORATOR_ALREADY_EXISTS)
                .map(officeBranch -> officeBranchWithRoles(officeBranch, info.getRoleIds()))
                .map(officeBranchWithRoles -> createNewCollaborator(
                        collaboratorId,
                        info.getEmail(),
                        info.getName(),
                        officeBranchWithRoles
                ))
                .flatMap(collaborator -> collaboratorRepo.store(collaborator)
                        .onSuccess(v -> invitationEmailSender.sendInvitation(collaborator))
                        .toEither(CollaboratorError.DB_ERROR));
    }
}

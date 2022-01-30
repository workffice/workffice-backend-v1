package controller;

import backoffice.domain.collaborator.Collaborator;
import backoffice.domain.collaborator.CollaboratorRepository;
import backoffice.domain.collaborator.Status;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Role;
import backoffice.domain.role.RoleRepository;
import backoffice.factories.CollaboratorBuilder;
import backoffice.factories.RoleBuilder;

import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CollaboratorUtil {
    @Autowired
    RoleRepository roleRepo;
    @Autowired
    CollaboratorRepository collaboratorRepo;

    public Collaborator createCollaborator(OfficeBranch officeBranch, Set<Permission> permissions) {
        var role = new RoleBuilder()
                .withOfficeBranch(officeBranch)
                .withPermissions(permissions).build();
        roleRepo.store(role);
        var collaborator = new CollaboratorBuilder()
                .withStatus(Status.ACTIVE)
                .withOfficeBranch(officeBranch)
                .addRole(role).build();
        collaboratorRepo.store(collaborator);
        return collaborator;
    }

    public Collaborator createCollaborator(OfficeBranch officeBranch, Role role) {
        var collaborator = new CollaboratorBuilder()
                .withStatus(Status.ACTIVE)
                .withOfficeBranch(officeBranch)
                .addRole(role).build();
        collaboratorRepo.store(collaborator);
        return collaborator;
    }
}

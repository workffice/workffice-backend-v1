package backoffice.infrastructure;

import backoffice.domain.collaborator.Collaborator;
import backoffice.domain.collaborator.CollaboratorId;
import backoffice.domain.collaborator.Status;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchRepository;
import backoffice.domain.office_holder.OfficeHolderRepository;
import backoffice.domain.role.RoleRepository;
import backoffice.factories.CollaboratorBuilder;
import backoffice.factories.OfficeBranchBuilder;
import backoffice.factories.RoleBuilder;
import com.google.common.collect.Sets;
import io.vavr.control.Option;
import io.vavr.control.Try;
import server.WorkfficeApplication;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = WorkfficeApplication.class)
public class TestCollaboratorMySQLRepo {

    @Autowired
    CollaboratorMySQLRepo collaboratorMySQLRepo;
    @Autowired
    RoleRepository roleRepo;
    @Autowired
    OfficeBranchRepository officeBranchRepo;
    @Autowired
    OfficeHolderRepository officeHolderRepo;

    private OfficeBranch createOfficeBranch() {
        var officeBranch = new OfficeBranchBuilder().build();
        officeHolderRepo.store(officeBranch.owner());
        officeBranchRepo.store(officeBranch);
        return officeBranch;
    }

    @Test
    void itShouldStoreCollaboratorWithInfoSpecified() {
        var collaboratorId = new CollaboratorId();
        var officeBranch = createOfficeBranch();
        var role = new RoleBuilder()
                .withOfficeBranch(officeBranch)
                .build();
        roleRepo.store(role);
        var collaborator = Collaborator.createNew(
                collaboratorId,
                "john@doe.com",
                "pirlonzio",
                Sets.newHashSet(role),
                officeBranch
        );

        Try<Void> result = collaboratorMySQLRepo.store(collaborator);

        assertThat(result.isFailure()).isFalse();
        var collaboratorSaved = collaboratorMySQLRepo
                .findById(collaboratorId).get();
        assertThat(collaboratorSaved.email()).isEqualTo("john@doe.com");
        assertThat(collaboratorSaved.id()).isEqualTo(collaboratorId);
        assertThat(collaboratorSaved.officeBranch().id()).isEqualTo(officeBranch.id());
    }

    @Test
    void itShouldReturnCollaboratorWithRoles() {
        var collaboratorId = new CollaboratorId();
        var officeBranch = createOfficeBranch();
        var role = new RoleBuilder()
                .withOfficeBranch(officeBranch)
                .build();
        var role2 = new RoleBuilder()
                .withOfficeBranch(officeBranch)
                .build();
        roleRepo.store(role);
        roleRepo.store(role2);
        var collaborator = Collaborator.createNew(
                collaboratorId,
                "john@doe.com",
                "pirlonzio",
                Sets.newHashSet(role, role2),
                officeBranch
        );
        collaboratorMySQLRepo.store(collaborator);

        var collaboratorSaved = collaboratorMySQLRepo
                .findWithRoles(collaboratorId).get();
        assertThat(collaboratorSaved.officeBranch().id()).isEqualTo(officeBranch.id());
        assertThat(collaboratorSaved.roles()).size().isEqualTo(2);
        assertThat(collaboratorSaved.roles()).containsExactlyInAnyOrder(role, role2);
    }

    @Test
    void itShouldReturnEmptyWhenCollaboratorDoesNotExist() {
        var collaboratorId = new CollaboratorId();

        Option<Collaborator> result1 = collaboratorMySQLRepo.findById(collaboratorId);
        Option<Collaborator> result2 = collaboratorMySQLRepo.findWithRoles(collaboratorId);

        assertThat(result1.isEmpty()).isTrue();
        assertThat(result2.isEmpty()).isTrue();
    }

    @Test
    void itShouldReturnCollaboratorWithRolesRelatedToOfficeBranch() {
        var officeBranch = createOfficeBranch();
        var role = new RoleBuilder()
                .withOfficeBranch(officeBranch)
                .build();
        var role2 = new RoleBuilder()
                .withOfficeBranch(officeBranch)
                .build();
        roleRepo.store(role);
        roleRepo.store(role2);
        var collaboratorId = new CollaboratorId();
        var collaborator = Collaborator.createNew(
                collaboratorId,
                "napoleon@gallardo.com",
                "pirlonzio",
                Sets.newHashSet(role, role2),
                officeBranch
        );
        collaboratorMySQLRepo.store(collaborator);

        Option<Collaborator> maybeCollaborator = collaboratorMySQLRepo.find(collaborator.email(), officeBranch);

        assertThat(maybeCollaborator.isDefined()).isTrue();
        assertThat(maybeCollaborator.get().roles()).size().isEqualTo(2);
    }

    @Test
    void itShouldReturnExistsTrueWhenThereIsACollaboratorWithEmailAndOfficeBranchSpecified() {
        var collaboratorId = new CollaboratorId();
        var officeBranch = createOfficeBranch();
        var role = new RoleBuilder()
                .withOfficeBranch(officeBranch)
                .build();
        roleRepo.store(role);
        var collaborator = Collaborator.createNew(
                collaboratorId,
                "napoleon@gallardo.com",
                "pirlonzio",
                Sets.newHashSet(role),
                officeBranch
        );
        collaboratorMySQLRepo.store(collaborator);

        assertThat(collaboratorMySQLRepo.exists("napoleon@gallardo.com", officeBranch))
                .isTrue();
    }

    @Test
    void itShouldReturnExistsFalseWhenThereIsNoCollaboratorWithEmailAndOfficeBranchSpecified() {
        var collaboratorId = new CollaboratorId();
        var officeBranch = createOfficeBranch();
        var officeBranchWithoutCollaborator = createOfficeBranch();
        var role = new RoleBuilder()
                .withOfficeBranch(officeBranch)
                .build();
        roleRepo.store(role);
        var collaborator = Collaborator.createNew(
                collaboratorId,
                "scaloneta@mail.com",
                "pirlonzio",
                Sets.newHashSet(role),
                officeBranch
        );
        collaboratorMySQLRepo.store(collaborator);

        assertThat(collaboratorMySQLRepo
                .exists("unexistent@mail.com", officeBranch)).isFalse();
        assertThat(collaboratorMySQLRepo
                .exists("scaloneta@mail.com", officeBranchWithoutCollaborator)).isFalse();
    }

    @Test
    void itShouldUpdateCollaborator() {
        var officeBranch = createOfficeBranch();
        var collaborator = new CollaboratorBuilder()
                .withStatus(Status.PENDING)
                .withOfficeBranch(officeBranch)
                .build();
        collaboratorMySQLRepo.store(collaborator);

        collaborator.activate();
        collaboratorMySQLRepo.update(collaborator);

        var collaboratorSaved = collaboratorMySQLRepo.findById(collaborator.id()).get();

        assertThat(collaboratorSaved.isActive()).isTrue();
    }

    @Test
    void itShouldReturnCollaboratorsRelatedWithOfficeBranch() {
        var officeBranch = createOfficeBranch();
        var collaborator = new CollaboratorBuilder()
                .withStatus(Status.PENDING)
                .withOfficeBranch(officeBranch)
                .build();
        var collaborator2 = new CollaboratorBuilder()
                .withStatus(Status.ACTIVE)
                .withOfficeBranch(officeBranch)
                .build();
        var collaborator3 = new CollaboratorBuilder()
                .withStatus(Status.INACTIVE)
                .withOfficeBranch(officeBranch)
                .build();
        collaboratorMySQLRepo.store(collaborator);
        collaboratorMySQLRepo.store(collaborator2);
        collaboratorMySQLRepo.store(collaborator3);

        List<Collaborator> collaborators = collaboratorMySQLRepo.find(officeBranch);

        assertThat(collaborators).size().isEqualTo(3);
        assertThat(collaborators).containsExactlyInAnyOrder(
                collaborator,
                collaborator2,
                collaborator3
        );
    }

    @Test
    void itShouldReturnActiveCollaboratorsThatHaveEmailSpecifiedWithOfficeBranch() {
        var officeBranch1 = createOfficeBranch();
        var officeBranch2 = createOfficeBranch();
        var officeBranch3 = createOfficeBranch();
        var officeBranch4 = createOfficeBranch();
        var collaborator = new CollaboratorBuilder()
                .withEmail("john@doe.com")
                .withStatus(Status.PENDING)
                .withOfficeBranch(officeBranch1)
                .build();
        var collaborator2 = new CollaboratorBuilder()
                .withEmail("john@doe.com")
                .withStatus(Status.ACTIVE)
                .withOfficeBranch(officeBranch2)
                .build();
        var collaborator3 = new CollaboratorBuilder()
                .withEmail("john@doe.com")
                .withStatus(Status.ACTIVE)
                .withOfficeBranch(officeBranch3)
                .build();
        var collaborator4 = new CollaboratorBuilder()
                .withEmail("john@doe.com")
                .withStatus(Status.INACTIVE)
                .withOfficeBranch(officeBranch4)
                .build();
        collaboratorMySQLRepo.store(collaborator);
        collaboratorMySQLRepo.store(collaborator2);
        collaboratorMySQLRepo.store(collaborator3);
        collaboratorMySQLRepo.store(collaborator4);

        var collaborators = collaboratorMySQLRepo.find("john@doe.com");

        assertThat(collaborators).size().isEqualTo(2);
        assertThat(collaborators).containsExactlyInAnyOrder(collaborator2, collaborator3);
        assertThat(collaborators).map(col -> col.officeBranch().id())
                .containsExactlyInAnyOrder(officeBranch2.id(), officeBranch3.id());
    }
}

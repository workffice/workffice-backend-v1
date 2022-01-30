package backoffice.application.collaborator;

import backoffice.application.dto.collaborator.CollaboratorError;
import backoffice.application.dto.collaborator.CollaboratorInformation;
import backoffice.application.dto.office_branch.OfficeBranchError;
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
import backoffice.domain.role.RoleRepository;
import backoffice.factories.OfficeBranchBuilder;
import backoffice.factories.RoleBuilder;
import com.google.common.collect.Lists;
import io.vavr.control.Either;
import io.vavr.control.Try;
import shared.application.UseCaseError;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.util.collections.Sets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestCollaboratorCreator {

    OfficeBranchFinder officeBranchFinder = mock(OfficeBranchFinder.class);
    CollaboratorRepository collaboratorRepo = mock(CollaboratorRepository.class);
    RoleRepository roleRepo = mock(RoleRepository.class);
    InvitationEmailSender invitationEmailSender = mock(InvitationEmailSender.class);
    ArgumentCaptor<Collaborator> collaboratorCaptor = ArgumentCaptor.forClass(Collaborator.class);

    CollaboratorCreator collaboratorCreator = new CollaboratorCreator(
            roleRepo,
            officeBranchFinder,
            collaboratorRepo,
            invitationEmailSender
    );

    @Test
    void itShouldReturnOfficeBranchNotExistWhenThereIsNoOfficeBranchWithIdSpecified() {
        var officeBranchId = new OfficeBranchId();
        var collaboratorInfo = CollaboratorInformation.of(Sets.newSet(UUID.randomUUID()), "some@mail.com", "pirlonzio");
        when(officeBranchFinder.findWithAuthorization(
                officeBranchId,
                Permission.create(Access.WRITE, Resource.COLLABORATOR)
        )).thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST));

        Either<UseCaseError, Void> response = collaboratorCreator.create(officeBranchId, new CollaboratorId(),
                collaboratorInfo);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft())
                .isEqualTo(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST);
    }

    @Test
    void itShouldReturnOfficeBranchForbiddenWhenAuthUserDoesNotHaveAccessToOfficeBranch() {
        var officeBranchId = new OfficeBranchId();
        var collaboratorInfo = CollaboratorInformation.of(
                Sets.newSet(UUID.randomUUID()),
                "some@mail.com",
                "pirlonzio"
        );
        when(officeBranchFinder.findWithAuthorization(
                officeBranchId,
                Permission.create(Access.WRITE, Resource.COLLABORATOR)
        )).thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN));

        Either<UseCaseError, Void> response = collaboratorCreator.create(officeBranchId, new CollaboratorId(),
                collaboratorInfo);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN);
    }

    @Test
    void itShouldReturnAlreadyExistErrorWhenThereIsACollaboratorWithTheSameEmailForOfficeBranch() {
        var officeBranch = new OfficeBranchBuilder().build();
        var collaboratorInfo = CollaboratorInformation.of(Sets.newSet(UUID.randomUUID()), "some@mail.com", "pirlonzio");
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.WRITE, Resource.COLLABORATOR)
        )).thenReturn(Either.right(officeBranch.toResponse()));
        when(collaboratorRepo.exists(contains(collaboratorInfo.getEmail()), any(OfficeBranch.class))).thenReturn(true);

        Either<UseCaseError, Void> response = collaboratorCreator.create(officeBranch.id(), new CollaboratorId(),
                collaboratorInfo);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(CollaboratorError.COLLABORATOR_ALREADY_EXISTS);
    }

    @Test
    void itShouldFilterRoleIdsThatDoesNotBelongToOfficeBranch() {
        var officeBranch = new OfficeBranchBuilder().build();
        var role1 = new RoleBuilder()
                .withOfficeBranch(officeBranch)
                .build();
        var role2 = new RoleBuilder().build();
        var roleIds = Sets.newSet(UUID.fromString(role1.id().toString()), UUID.fromString(role2.id().toString()));
        var collaboratorInfo = CollaboratorInformation.of(roleIds, "bla@mail.com", "pirlonzio");
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.WRITE, Resource.COLLABORATOR)
        )).thenReturn(Either.right(officeBranch.toResponse()));
        when(collaboratorRepo.exists(contains(collaboratorInfo.getEmail()), any(OfficeBranch.class))).thenReturn(false);
        when(roleRepo.findByOfficeBranch(any(OfficeBranch.class))).thenReturn(Lists.newArrayList(role1));
        when(collaboratorRepo.store(any())).thenReturn(Try.success(null));

        collaboratorCreator.create(officeBranch.id(), new CollaboratorId(), collaboratorInfo);

        verify(collaboratorRepo, times(1))
                .store(collaboratorCaptor.capture());
        var collaboratorStored = collaboratorCaptor.getValue();
        assertThat(collaboratorStored.email()).isEqualTo("bla@mail.com");
        assertThat(collaboratorStored.roles()).size().isEqualTo(1);
        assertThat(collaboratorStored.roles()).containsExactly(Role.create(
                role1.id(),
                role1.name(),
                role1.permissions(),
                officeBranch));
    }

    @Test
    void itShouldCreateCollaboratorWithoutRolesIfRolesAreNotSpecified() {
        var officeBranch = new OfficeBranchBuilder().build();
        var collaboratorInfo = CollaboratorInformation.of(Sets.newSet(), "bla@mail.com", "pirlonzio");
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.WRITE, Resource.COLLABORATOR)
        )).thenReturn(Either.right(officeBranch.toResponse()));
        when(collaboratorRepo.exists(contains(collaboratorInfo.getEmail()), any(OfficeBranch.class))).thenReturn(false);
        when(collaboratorRepo.store(any())).thenReturn(Try.success(null));

        collaboratorCreator.create(officeBranch.id(), new CollaboratorId(), collaboratorInfo);

        verify(collaboratorRepo, times(1)).store(collaboratorCaptor.capture());
        var collaboratorStored = collaboratorCaptor.getValue();
        assertThat(collaboratorStored.roles()).isEmpty();
    }

    @Test
    void itShouldSendEmailAfterStoreCollaborator() {
        var officeBranch = new OfficeBranchBuilder().build();
        var collaboratorInfo = CollaboratorInformation.of(Sets.newSet(), "bla@mail.com", "pirlonzio");
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.WRITE, Resource.COLLABORATOR)
        )).thenReturn(Either.right(officeBranch.toResponse()));
        when(collaboratorRepo.exists(contains(collaboratorInfo.getEmail()), any(OfficeBranch.class))).thenReturn(false);
        when(collaboratorRepo.store(any())).thenReturn(Try.success(null));

        collaboratorCreator.create(officeBranch.id(), new CollaboratorId(), collaboratorInfo);

        verify(invitationEmailSender, times(1)).sendInvitation(any(Collaborator.class));
    }
}

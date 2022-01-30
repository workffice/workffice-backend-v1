package backoffice.application.collaborator;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.collaborator.CollaboratorError;
import backoffice.application.dto.collaborator.CollaboratorResponse;
import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.domain.collaborator.CollaboratorId;
import backoffice.domain.collaborator.CollaboratorRepository;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.factories.CollaboratorBuilder;
import io.vavr.control.Either;
import io.vavr.control.Option;
import shared.application.UseCaseError;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestCollaboratorFinder {
    PermissionValidator permissionValidator = mock(PermissionValidator.class);
    CollaboratorRepository collaboratorRepo = mock(CollaboratorRepository.class);

    CollaboratorFinder finder = new CollaboratorFinder(permissionValidator, collaboratorRepo);

    @Test
    void itShouldReturnCollaboratorNotFoundWhenThereIsNoCollaboratorWithSpecifiedId() {
        var collaboratorId = new CollaboratorId();
        when(collaboratorRepo.findById(collaboratorId)).thenReturn(Option.none());

        Either<UseCaseError, CollaboratorResponse> response = finder.find(collaboratorId);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(CollaboratorError.COLLABORATOR_NOT_FOUND);
    }

    @Test
    void itShouldReturnOfficeBranchForbiddenWhenAuthUserDoesNotHaveAccessToCollaborators() {
        var collaborator = new CollaboratorBuilder().build();
        when(collaboratorRepo.findById(collaborator.id())).thenReturn(Option.of(collaborator));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.READ, Resource.COLLABORATOR)))
        ).thenReturn(false);

        Either<UseCaseError, CollaboratorResponse> response = finder.find(collaborator.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN);
    }

    @Test
    void itShouldReturnCollaboratorInformation() {
        var collaborator = new CollaboratorBuilder().build();
        when(collaboratorRepo.findById(collaborator.id())).thenReturn(Option.of(collaborator));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.READ, Resource.COLLABORATOR)))
        ).thenReturn(true);

        Either<UseCaseError, CollaboratorResponse> response = finder.find(collaborator.id());

        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).isEqualTo(collaborator.toResponse());
    }
}

package backoffice.application.collaborator;

import backoffice.application.dto.collaborator.CollaboratorResponse;
import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.collaborator.CollaboratorRepository;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.factories.CollaboratorBuilder;
import backoffice.factories.OfficeBranchBuilder;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestCollaboratorsFinder {
    OfficeBranchFinder officeBranchFinder = mock(OfficeBranchFinder.class);
    CollaboratorRepository collaboratorRepo = mock(CollaboratorRepository.class);

    CollaboratorsFinder finder = new CollaboratorsFinder(officeBranchFinder, collaboratorRepo);

    @Test
    void itShouldReturnNotFoundWhenThereIsNoOfficeBranchWithIdSpecified() {
        var officeBranchId = new OfficeBranchId();
        when(officeBranchFinder.findWithAuthorization(
                officeBranchId,
                Permission.create(Access.READ, Resource.COLLABORATOR)
        )).thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST));

        Either<UseCaseError, List<CollaboratorResponse>> response = finder.find(officeBranchId);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST);
    }

    @Test
    void itShouldReturnForbiddenWhenAuthUserDoesNotHaveAccessToReadCollaborators() {
        var officeBranchId = new OfficeBranchId();
        when(officeBranchFinder.findWithAuthorization(
                officeBranchId,
                Permission.create(Access.READ, Resource.COLLABORATOR)
        )).thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN));

        Either<UseCaseError, List<CollaboratorResponse>> response = finder.find(officeBranchId);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN);
    }

    @Test
    void itShouldReturnCollaboratorsRelatedWithOfficeBranch() {
        var officeBranch = new OfficeBranchBuilder().build();
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.READ, Resource.COLLABORATOR)
        )).thenReturn(Either.right(officeBranch.toResponse()));
        var collaborator1 = new CollaboratorBuilder().build();
        var collaborator2 = new CollaboratorBuilder().build();
        var collaborator3 = new CollaboratorBuilder().build();
        when(collaboratorRepo.find(any(OfficeBranch.class))).thenReturn(
                ImmutableList.of(collaborator1, collaborator2, collaborator3)
        );

        Either<UseCaseError, List<CollaboratorResponse>> response = finder.find(officeBranch.id());

        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).containsExactlyInAnyOrder(
                collaborator1.toResponse(),
                collaborator2.toResponse(),
                collaborator3.toResponse()
        );
    }
}

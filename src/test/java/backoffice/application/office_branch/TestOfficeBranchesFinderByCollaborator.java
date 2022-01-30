package backoffice.application.office_branch;

import authentication.application.AuthUserValidator;
import backoffice.application.dto.collaborator.CollaboratorError;
import backoffice.application.dto.office_branch.OfficeBranchResponse;
import backoffice.domain.collaborator.CollaboratorRepository;
import backoffice.domain.office_branch.OfficeBranchRepository;
import backoffice.factories.CollaboratorBuilder;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Either;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestOfficeBranchesFinderByCollaborator {
    CollaboratorRepository collaboratorRepo = mock(CollaboratorRepository.class);
    OfficeBranchRepository officeBranchRepo = mock(OfficeBranchRepository.class);
    AuthUserValidator authUserValidator = mock(AuthUserValidator.class);

    OfficeBranchesFinderByCollaborator officeBranchesFinderByCollaborator = new OfficeBranchesFinderByCollaborator(
            authUserValidator,
            collaboratorRepo,
            officeBranchRepo
    );

    @Test
    void itShouldReturnForbiddenWhenAuthUserHasNotTheSameEmailAsCollaboratorRequested() {
        when(authUserValidator.isSameUserAsAuthenticated("john@doe.com")).thenReturn(false);

        Either<CollaboratorError, List<OfficeBranchResponse>> response = officeBranchesFinderByCollaborator
                .find("john@doe.com");

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(CollaboratorError.FORBIDDEN);
    }

    @Test
    void itShouldReturnNotFoundWhenThereIsNoCollaboratorWithEmailSpecified() {
        when(authUserValidator.isSameUserAsAuthenticated("john@doe.com")).thenReturn(true);
        when(collaboratorRepo.find("john@doe.com")).thenReturn(new ArrayList<>());

        Either<CollaboratorError, List<OfficeBranchResponse>> response = officeBranchesFinderByCollaborator
                .find("john@doe.com");

        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).isEmpty();
    }


    @Test
    void itShouldReturnOfficeBranchesFoundForCollaboratorEmailSpecified() {
        var collaborator1 = new CollaboratorBuilder()
                .withEmail("john@doe.com")
                .build();
        var collaborator2 = new CollaboratorBuilder()
                .withEmail("john@doe.com")
                .build();
        when(authUserValidator.isSameUserAsAuthenticated("john@doe.com")).thenReturn(true);
        when(collaboratorRepo.find("john@doe.com"))
                .thenReturn(ImmutableList.of(collaborator1, collaborator2));
        when(officeBranchRepo.findByIds(ImmutableList.of(
                collaborator1.officeBranch().id(),
                collaborator2.officeBranch().id()
        ))).thenReturn(ImmutableList.of(collaborator1.officeBranch(), collaborator2.officeBranch()));

        Either<CollaboratorError, List<OfficeBranchResponse>> response = officeBranchesFinderByCollaborator
                .find("john@doe.com");

        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).size().isEqualTo(2);
        assertThat(response.get()).containsExactlyInAnyOrder(
                collaborator1.officeBranch().toResponse(),
                collaborator2.officeBranch().toResponse()
        );
    }
}

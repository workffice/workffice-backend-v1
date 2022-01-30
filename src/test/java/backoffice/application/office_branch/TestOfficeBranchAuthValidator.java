package backoffice.application.office_branch;

import authentication.application.AuthUserValidator;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_holder.OfficeHolderRepository;
import backoffice.factories.OfficeBranchBuilder;
import io.vavr.control.Option;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestOfficeBranchAuthValidator {

    OfficeHolderRepository officeHolderRepo = mock(OfficeHolderRepository.class);
    AuthUserValidator authUserValidator = mock(AuthUserValidator.class);

    @Test
    void itShouldReturnFalseIfThereIsNoOfficeHolderRelatedWithOfficeBranch() {
        OfficeBranchAuthValidator validator = new OfficeBranchAuthValidator(authUserValidator, officeHolderRepo);
        OfficeBranch officeBranch = new OfficeBranchBuilder().build();
        when(officeHolderRepo.findByOfficeBranch(any()))
                .thenReturn(Option.none());

        boolean hasAccess = validator.authUserIsOwner(officeBranch);

        assertThat(hasAccess).isFalse();
    }

    @Test
    void itShouldReturnFalseIfOfficeHolderEmailIsNotTheSameAsTheAuthenticatedUser() {
        OfficeBranchAuthValidator validator = new OfficeBranchAuthValidator(authUserValidator, officeHolderRepo);
        OfficeBranch officeBranch = new OfficeBranchBuilder().build();
        when(officeHolderRepo.findByOfficeBranch(any()))
                .thenReturn(Option.of(officeBranch.owner()));
        when(authUserValidator.isSameUserAsAuthenticated(officeBranch.owner().email()))
                .thenReturn(false);

        boolean hasAccess = validator.authUserIsOwner(officeBranch);

        assertThat(hasAccess).isFalse();
    }

    @Test
    void itShouldReturnTrueIfOfficeHolderEmailIsTheSameAsAuthenticatedUser() {
        OfficeBranchAuthValidator validator = new OfficeBranchAuthValidator(authUserValidator, officeHolderRepo);
        OfficeBranch officeBranch = new OfficeBranchBuilder().build();
        when(officeHolderRepo.findByOfficeBranch(any()))
                .thenReturn(Option.of(officeBranch.owner()));
        when(authUserValidator.isSameUserAsAuthenticated(officeBranch.owner().email()))
                .thenReturn(true);

        boolean hasAccess = validator.authUserIsOwner(officeBranch);

        assertThat(hasAccess).isTrue();
    }
}

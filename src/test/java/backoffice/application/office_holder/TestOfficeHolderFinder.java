package backoffice.application.office_holder;

import authentication.application.AuthUserValidator;
import backoffice.application.dto.office_holder.OfficeHolderError;
import backoffice.application.dto.office_holder.OfficeHolderResponse;
import backoffice.domain.office_holder.OfficeHolder;
import backoffice.domain.office_holder.OfficeHolderId;
import backoffice.domain.office_holder.OfficeHolderRepository;
import backoffice.factories.OfficeHolderBuilder;
import io.vavr.control.Option;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestOfficeHolderFinder {
    OfficeHolderRepository repo = mock(OfficeHolderRepository.class);
    AuthUserValidator authUserValidator = mock(AuthUserValidator.class);

    @Test
    void itShouldReturnNotFoundWhenThereIsNoOfficeHolderWithIdProvided() {
        OfficeHolderId id = new OfficeHolderId();
        when(repo.findById(id)).thenReturn(Option.none());
        OfficeHolderFinder officeHolderFinder = new OfficeHolderFinder(authUserValidator, repo);

        var response = officeHolderFinder.find(id);

        assertThat(response.getLeft()).isEqualTo(OfficeHolderError.OFFICE_HOLDER_NOT_FOUND);
    }

    @Test
    void itShouldReturnOfficeHolderInformation() {
        OfficeHolderId id = new OfficeHolderId();
        OfficeHolder officeHolder = new OfficeHolderBuilder().withId(id).build();
        when(repo.findById(id)).thenReturn(Option.of(officeHolder));
        when(authUserValidator.isSameUserAsAuthenticated(anyString())).thenReturn(true);
        OfficeHolderFinder officeHolderFinder = new OfficeHolderFinder(authUserValidator, repo);

        var response = officeHolderFinder.find(id);

        assertThat(response.get())
                .isEqualTo(OfficeHolderResponse.of(id.toString(), officeHolder.email()));
    }

    @Test
    void itShouldReturnNotAuthorizedWhenOfficeHolderEmailIsNotTheSameAsUserAuthenticated() {
        OfficeHolderId id = new OfficeHolderId();
        OfficeHolder officeHolder = new OfficeHolderBuilder()
                .withId(id)
                .withEmail("invalid@email.com")
                .build();
        when(repo.findById(id)).thenReturn(Option.of(officeHolder));
        when(authUserValidator.isSameUserAsAuthenticated("invalid@email.com")).thenReturn(false);
        OfficeHolderFinder officeHolderFinder = new OfficeHolderFinder(authUserValidator, repo);

        var response = officeHolderFinder.find(id);

        assertThat(response.getLeft()).isEqualTo(OfficeHolderError.OFFICE_HOLDER_FORBIDDEN);
    }
}

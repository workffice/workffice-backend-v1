package backoffice.application;

import backoffice.domain.collaborator.CollaboratorRepository;
import backoffice.domain.office_holder.OfficeHolderRepository;
import backoffice.factories.CollaboratorBuilder;
import backoffice.factories.OfficeHolderBuilder;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Option;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

import static backoffice.application.UserTypeResolver.UserType.COLLABORATOR;
import static backoffice.application.UserTypeResolver.UserType.OFFICE_HOLDER;
import static backoffice.application.UserTypeResolver.UserType.RENTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUserTypeResolver {
    CollaboratorRepository collaboratorRepo = mock(CollaboratorRepository.class);
    OfficeHolderRepository officeHolderRepo = mock(OfficeHolderRepository.class);

    UserTypeResolver userTypeResolver = new UserTypeResolver(officeHolderRepo, collaboratorRepo);

    @Test
    void itShouldReturnOfficeHolderWhenThereIsAnOfficeHolderWithEmailProvided() {
        var officeHolder = new OfficeHolderBuilder().build();
        when(officeHolderRepo.find("john@doe.com")).thenReturn(Option.of(officeHolder));

        UserTypeResolver.UserType type = userTypeResolver.getUserType("john@doe.com");

        assertThat(type).isEqualTo(OFFICE_HOLDER);
    }

    @Test
    void itShouldReturnCollaboratorIfThereIsNoOfficeHolderAndThereIsAtLeastOneCollaboratorWithEmail() {
        var collaborator = new CollaboratorBuilder().build();
        when(officeHolderRepo.find("john@doe.com")).thenReturn(Option.none());
        when(collaboratorRepo.find("john@doe.com")).thenReturn(ImmutableList.of(collaborator));

        UserTypeResolver.UserType type = userTypeResolver.getUserType("john@doe.com");

        assertThat(type).isEqualTo(COLLABORATOR);
    }

    @Test
    void itShouldReturnRenterWhenThereIsNoOfficeHolderAndNoCollaboratorWithEmailProvided() {
        when(officeHolderRepo.find("john@doe.com")).thenReturn(Option.none());
        when(collaboratorRepo.find("john@doe.com")).thenReturn(new ArrayList<>());

        UserTypeResolver.UserType type = userTypeResolver.getUserType("john@doe.com");

        assertThat(type).isEqualTo(RENTER);
    }
}

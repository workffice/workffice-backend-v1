package backoffice.application.office_branch;

import authentication.application.AuthUserValidator;
import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.dto.office_branch.OfficeBranchInformation;
import backoffice.application.dto.office_holder.OfficeHolderError;
import backoffice.domain.office_branch.Image;
import backoffice.domain.office_branch.Location;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchCreatedEvent;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.office_branch.OfficeBranchRepository;
import backoffice.domain.office_holder.OfficeHolder;
import backoffice.domain.office_holder.OfficeHolderId;
import backoffice.domain.office_holder.OfficeHolderRepository;
import backoffice.factories.OfficeHolderBuilder;
import io.vavr.control.Option;
import io.vavr.control.Try;
import shared.domain.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import javax.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestOfficeBranchCreator {
    OfficeBranchRepository mockOfficeBranchRepo = mock(OfficeBranchRepository.class);
    OfficeHolderRepository mockOfficeHolderRepo = mock(OfficeHolderRepository.class);
    AuthUserValidator mockAuthUserValidator = mock(AuthUserValidator.class);
    EventBus eventBus = mock(EventBus.class);
    ArgumentCaptor<OfficeBranch> officeBranchCaptor = ArgumentCaptor.forClass(OfficeBranch.class);

    OfficeBranchCreator creator = new OfficeBranchCreator(
            eventBus,
            mockAuthUserValidator,
            mockOfficeBranchRepo,
            mockOfficeHolderRepo
    );

    private static OfficeBranchInformation emptyOfficeBranchInfo() {
        return OfficeBranchInformation.of(
                "",
                "",
                "",
                new ArrayList<>(),
                "",
                "",
                "",
                ""
        );
    }

    @Test
    void itShouldReturnOfficeHolderNotFoundWhenItDoesNotExist() {
        OfficeHolderId officeHolderId = new OfficeHolderId();
        when(mockOfficeHolderRepo.findById(officeHolderId)).thenReturn(Option.none());

        var response = creator.create(officeHolderId, new OfficeBranchId(),
                emptyOfficeBranchInfo());

        assertThat(response.getLeft()).isEqualTo(OfficeHolderError.OFFICE_HOLDER_NOT_FOUND);
    }

    @Test
    void itShouldReturnForbiddenWhenUserAuthenticatedDoesNotOwnOfficeHolder() {
        OfficeHolder officeHolder = new OfficeHolderBuilder().build();
        when(mockAuthUserValidator.isSameUserAsAuthenticated(officeHolder.email())).thenReturn(false);
        when(mockOfficeHolderRepo.findById(officeHolder.id())).thenReturn(Option.of(officeHolder));

        var response = creator.create(officeHolder.id(), new OfficeBranchId(),
                emptyOfficeBranchInfo());

        assertThat(response.getLeft()).isEqualTo(OfficeHolderError.OFFICE_HOLDER_FORBIDDEN);
    }

    @Test
    void itShouldReturnDBErrorWhenOfficeBranchStoreFails() {
        OfficeHolder officeHolder = new OfficeHolderBuilder().build();
        when(mockAuthUserValidator.isSameUserAsAuthenticated(officeHolder.email())).thenReturn(true);
        when(mockOfficeHolderRepo.findById(officeHolder.id())).thenReturn(Option.of(officeHolder));
        when(mockOfficeBranchRepo.store(any())).thenReturn(Try.failure(new PersistenceException()));

        var response = creator.create(officeHolder.id(), new OfficeBranchId(),
                emptyOfficeBranchInfo());

        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.DB_ERROR);
    }

    @Test
    void itShouldStoreOfficeBranchWithAttributesSpecified() {
        OfficeHolder officeHolder = new OfficeHolderBuilder().build();
        when(mockOfficeBranchRepo.store(any())).thenReturn(Try.success(null));
        when(mockAuthUserValidator.isSameUserAsAuthenticated(officeHolder.email())).thenReturn(true);
        when(mockOfficeHolderRepo.findById(officeHolder.id())).thenReturn(Option.of(officeHolder));
        OfficeBranchId officeBranchId = new OfficeBranchId();
        OfficeBranchInformation info = OfficeBranchInformation.of(
                "Monumental",
                "Some description",
                "123456789",
                Arrays.asList("image1.com", "image2.com"),
                "Mendoza",
                "Godoy Cruz",
                "Calle falsa",
                "5501"
        );

        var response = creator.create(officeHolder.id(), officeBranchId, info);

        verify(mockOfficeBranchRepo, times(1)).store(officeBranchCaptor.capture());
        OfficeBranch officeBranchSaved = officeBranchCaptor.getValue();

        assertThat(response.isRight()).isTrue();
        assertThat(officeBranchSaved.id()).isEqualTo(officeBranchId);
        assertThat(officeBranchSaved.name()).isEqualTo("Monumental");
        assertThat(officeBranchSaved.description()).isEqualTo("Some description");
        assertThat(officeBranchSaved.phone()).isEqualTo("123456789");
        assertThat(officeBranchSaved.owner()).isEqualTo(officeHolder);
        assertThat(officeBranchSaved.location()).isEqualTo(new Location(
                "Mendoza",
                "Godoy Cruz",
                "Calle falsa",
                "5501")
        );
        assertThat(officeBranchSaved.images())
                .containsExactly(new Image("image1.com"), new Image("image2.com"));
    }

    @Test
    void itShouldPublishOfficeBranchCreatedWhenItIsStoredSuccesfully() {
        OfficeHolder officeHolder = new OfficeHolderBuilder().build();
        when(mockOfficeBranchRepo.store(any())).thenReturn(Try.success(null));
        when(mockAuthUserValidator.isSameUserAsAuthenticated(officeHolder.email())).thenReturn(true);
        when(mockOfficeHolderRepo.findById(officeHolder.id())).thenReturn(Option.of(officeHolder));
        OfficeBranchId officeBranchId = new OfficeBranchId();
        OfficeBranchInformation info = OfficeBranchInformation.of(
                "Monumental",
                "Some description",
                "123456789",
                Arrays.asList("image1.com", "image2.com"),
                "Mendoza",
                "Godoy Cruz",
                "Calle falsa",
                "5501"
        );

        creator.create(officeHolder.id(), officeBranchId, info);

        verify(eventBus, times(1)).publish(OfficeBranchCreatedEvent.of(
                officeBranchId.toString(),
                officeHolder.id().toString(),
                "Monumental",
                "Mendoza",
                "Godoy Cruz",
                "Calle falsa",
                "123456789",
                Arrays.asList("image1.com", "image2.com")
        ));
    }
}

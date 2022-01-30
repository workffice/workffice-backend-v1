package backoffice.application.office;

import backoffice.application.dto.office.OfficeError;
import backoffice.application.dto.office.OfficeInformation;
import backoffice.application.dto.office.OfficeResponse;
import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.office.Office;
import backoffice.domain.office.OfficeCreatedEvent;
import backoffice.domain.office.OfficeId;
import backoffice.domain.office.OfficeRepository;
import backoffice.domain.office.Privacy;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.factories.OfficeBranchBuilder;
import io.vavr.control.Either;
import io.vavr.control.Try;
import shared.application.UseCaseError;
import shared.domain.EventBus;

import javax.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestOfficeCreator {
    EventBus eventBus = mock(EventBus.class);
    OfficeRepository officeRepo = mock(OfficeRepository.class);
    OfficeBranchFinder officeBranchFinder = mock(OfficeBranchFinder.class);
    ArgumentCaptor<Office> officeArgCaptor = ArgumentCaptor.forClass(Office.class);

    OfficeCreator officeCreator = new OfficeCreator(eventBus, officeRepo, officeBranchFinder);

    @Test
    void itShouldReturnOfficeBranchNotFoundWhenOfficeBranchDoesNotExist() {
        OfficeId officeId = new OfficeId();
        OfficeBranchId officeBranchId = new OfficeBranchId();
        OfficeInformation info = OfficeInformation.of(
                "some name",
                "some desc",
                10,
                10,
                "PRIVATE",
                "imageurl",
                10,
                10
        );
        when(officeBranchFinder.findWithAuthorization(
                officeBranchId,
                Permission.create(Access.WRITE, Resource.OFFICE)
        )).thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST));

        var response = officeCreator.create(officeId, officeBranchId, info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST);
    }

    @Test
    void itShouldReturnForbiddenWhenAuthenticatedUserHasNoAccessToOfficeBranch() {
        OfficeId officeId = new OfficeId();
        OfficeBranch officeBranch = new OfficeBranchBuilder().build();
        OfficeInformation info = OfficeInformation.of(
                "some name",
                "some desc",
                10,
                10,
                "PRIVATE",
                "imageurl",
                10,
                10
        );
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.WRITE, Resource.OFFICE)
        )).thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN));

        var response = officeCreator.create(officeId, officeBranch.id(), info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN);
    }

    @Test
    void itShouldReturnDBErrorWhenStoreFails() {
        OfficeId officeId = new OfficeId();
        OfficeBranch officeBranch = new OfficeBranchBuilder().build();
        OfficeInformation info = OfficeInformation.of(
                "some name",
                "some desc",
                10,
                10,
                "PRIVATE",
                "imageurl",
                10,
                10
        );
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.WRITE, Resource.OFFICE)
        )).thenReturn(Either.right(officeBranch.toResponse()));
        when(officeRepo.store(any())).thenReturn(Try.failure(new PersistenceException()));

        Either<UseCaseError, Void> response = officeCreator.create(officeId, officeBranch.id(), info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeError.DB_ERROR);
    }

    @Test
    void itShouldCallStoreOffice() {
        OfficeId officeId = new OfficeId();
        OfficeBranch officeBranch = new OfficeBranchBuilder().build();
        OfficeInformation info = OfficeInformation.of(
                "some name",
                "some desc",
                10,
                10,
                "PRIVATE",
                "imageurl",
                10,
                10
        );
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.WRITE, Resource.OFFICE)
        )).thenReturn(Either.right(officeBranch.toResponse()));
        when(officeRepo.store(any())).thenReturn(Try.success(null));

        officeCreator.create(officeId, officeBranch.id(), info);

        verify(officeRepo, times(1)).store(officeArgCaptor.capture());
        Office officeStored = officeArgCaptor.getValue();
        var officeResponse = officeStored.toResponse();
        assertThat(officeResponse.getId()).isEqualTo(officeId.toString());
        assertThat(officeResponse.getName()).isEqualTo("some name");
        assertThat(officeResponse.getDescription()).isEqualTo("some desc");
        assertThat(officeResponse.getCapacity()).isEqualTo(10);
        assertThat(officeResponse.getPrice()).isEqualTo(10);
        assertThat(officeResponse.getPrivacy()).isEqualTo(Privacy.PRIVATE.name());
        assertThat(officeResponse.getImageUrl()).isEqualTo("imageurl");
        assertThat(officeResponse.getTable()).isEqualTo(OfficeResponse.TableResponse.of(10, 10));
    }

    @Test
    void itShouldCreateOfficeWithSharedPrivacyWhenPrivacyIsInvalid() {
        OfficeId officeId = new OfficeId();
        OfficeBranch officeBranch = new OfficeBranchBuilder().build();
        OfficeInformation info = OfficeInformation.of(
                "some name",
                "some desc",
                10,
                10,
                "INVALID_PRIVACY",
                "imageurl",
                10,
                10
        );
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.WRITE, Resource.OFFICE)
        )).thenReturn(Either.right(officeBranch.toResponse()));
        when(officeRepo.store(any())).thenReturn(Try.success(null));

        officeCreator.create(officeId, officeBranch.id(), info);

        verify(officeRepo, times(1)).store(officeArgCaptor.capture());
        Office officeStored = officeArgCaptor.getValue();
        assertThat(officeStored.toResponse().getPrivacy()).isEqualTo(Privacy.SHARED.name());
    }

    @Test
    void itShouldReturnPrivateOfficeWithoutTablesWhenTablePropertiesAreNull() {
        OfficeId officeId = new OfficeId();
        OfficeBranch officeBranch = new OfficeBranchBuilder().build();
        OfficeInformation info = OfficeInformation.of(
                "some name",
                "some desc",
                10,
                10,
                "SHARED",
                "imageurl",
                null,
                null
        );
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.WRITE, Resource.OFFICE)
        )).thenReturn(Either.right(officeBranch.toResponse()));
        when(officeRepo.store(any())).thenReturn(Try.success(null));

        var response = officeCreator.create(officeId, officeBranch.id(), info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeError.SHARED_OFFICE_WITHOUT_TABLES);
    }

    @Test
    void itShouldPublishOfficeCreatedEventWhenItIsStoredSuccessfully() {
        OfficeId officeId = new OfficeId();
        OfficeBranch officeBranch = new OfficeBranchBuilder().build();
        OfficeInformation info = OfficeInformation.of(
                "some name",
                "some desc",
                100,
                1000,
                "PRIVATE",
                "imageurl",
                10,
                10
        );
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.WRITE, Resource.OFFICE)
        )).thenReturn(Either.right(officeBranch.toResponse()));
        when(officeRepo.store(any())).thenReturn(Try.success(null));

        officeCreator.create(officeId, officeBranch.id(), info);

        verify(eventBus, times(1)).publish(OfficeCreatedEvent.of(
                officeId.toString(),
                officeBranch.id().toString(),
                "some name",
                "PRIVATE",
                1000,
                100,
                10,
                10
        ));
    }

    @Test
    void itShouldPublishOfficeCreatedEventWithTableQuantityAndCapacity0WhenItIsNotSpecified() {
        OfficeId officeId = new OfficeId();
        OfficeBranch officeBranch = new OfficeBranchBuilder().build();
        OfficeInformation info = OfficeInformation.of(
                "some name",
                "some desc",
                100,
                1000,
                "PRIVATE",
                "imageurl",
                null,
                null
        );
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.WRITE, Resource.OFFICE)
        )).thenReturn(Either.right(officeBranch.toResponse()));
        when(officeRepo.store(any())).thenReturn(Try.success(null));

        officeCreator.create(officeId, officeBranch.id(), info);

        verify(eventBus, times(1)).publish(OfficeCreatedEvent.of(
                officeId.toString(),
                officeBranch.id().toString(),
                "some name",
                "PRIVATE",
                1000,
                100,
                0,
                0
        ));
    }
}

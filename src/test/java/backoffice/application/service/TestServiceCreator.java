package backoffice.application.service;

import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.dto.service.ServiceError;
import backoffice.application.dto.service.ServiceInformation;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.domain.service.Service;
import backoffice.domain.service.ServiceId;
import backoffice.domain.service.ServiceRepository;
import backoffice.factories.OfficeBranchBuilder;
import io.vavr.control.Either;
import io.vavr.control.Try;
import shared.application.UseCaseError;

import javax.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestServiceCreator {

    ServiceRepository serviceRepository = mock(ServiceRepository.class);
    OfficeBranchFinder officeBranchFinder = mock(OfficeBranchFinder.class);
    ArgumentCaptor<Service> officeBranchServiceArgCaptor = ArgumentCaptor.
            forClass(Service.class);

    ServiceCreator serviceCreator = new ServiceCreator(serviceRepository, officeBranchFinder);

    @Test
    void itShouldReturnOfficeBranchNotFoundWhenOfficeBranchDoesNotExist() {
        OfficeBranchId officeBranchId = new OfficeBranchId();
        ServiceId serviceId = new ServiceId();
        ServiceInformation serviceInformation = ServiceInformation.of(
                "Some name",
                "FOOD"
        );
        when(officeBranchFinder.findWithAuthorization(
                officeBranchId,
                Permission.create(Access.WRITE, Resource.SERVICE)
        )).thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST));

        var response = serviceCreator.createService(
                serviceId,
                serviceInformation,
                officeBranchId);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST);
    }

    @Test
    void itShouldReturnForbiddenWhenAuthenticatedUserHasNoAccessToOfficeBranch() {
        OfficeBranch officeBranch = new OfficeBranchBuilder().build();
        ServiceId serviceId = new ServiceId();
        ServiceInformation serviceInformation = ServiceInformation.of(
                "Some name",
                "FOOD"
        );
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.WRITE, Resource.SERVICE)
        )).thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN));

        var response = serviceCreator.createService(
                serviceId,
                serviceInformation,
                officeBranch.id());


        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN);
    }

    @Test
    void itShouldReturnDBErrorWhenStoreFails() {

        ServiceId serviceId = new ServiceId();
        OfficeBranch officeBranch = new OfficeBranchBuilder().build();
        ServiceInformation serviceInformation = ServiceInformation.of(
                "Some name",
                "FOOD"
        );

        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.WRITE, Resource.SERVICE)
        )).thenReturn(Either.right(officeBranch.toResponse()));
        when(serviceRepository.store(any())).thenReturn(Try.failure(new PersistenceException()));

        Either<UseCaseError, Void> response = serviceCreator.createService(
                serviceId,
                serviceInformation,
                officeBranch.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(ServiceError.DB_ERROR);
    }

    @Test
    void itShouldCallStoreService() {
        ServiceId serviceId = new ServiceId();
        OfficeBranch officeBranch = new OfficeBranchBuilder().build();
        ServiceInformation serviceInformation = ServiceInformation.of(
                "Some name",
                "FOOD"
        );
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.WRITE, Resource.SERVICE)
        )).thenReturn(Either.right(officeBranch.toResponse()));
        when(serviceRepository.store(any())).thenReturn(Try.success(null));

        serviceCreator.createService(serviceId, serviceInformation, officeBranch.id());

        verify(serviceRepository, times(1))
                .store(officeBranchServiceArgCaptor.capture());
        Service serviceStored = officeBranchServiceArgCaptor.getValue();
        var serviceResponse = serviceStored.toResponse();
        assertThat(serviceResponse.getId()).isEqualTo(serviceId.toString());
        assertThat(serviceResponse.getName()).isEqualTo("Some name");
        assertThat(serviceResponse.getCategory()).isEqualTo("FOOD");
    }
}

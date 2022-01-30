package backoffice.application.service;

import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.dto.service.ServiceResponse;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.service.ServiceRepository;
import backoffice.factories.OfficeBranchBuilder;
import backoffice.factories.ServiceBuilder;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Either;
import io.vavr.control.Option;
import shared.application.UseCaseError;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestServiceFinder {

    ServiceRepository serviceRepo = mock(ServiceRepository.class);
    OfficeBranchFinder officeBranchFinder = mock(OfficeBranchFinder.class);

    ServiceFinder serviceFinder = new ServiceFinder(
            serviceRepo,
            officeBranchFinder);

    @Test
    void itShouldReturnOfficeBranchNotFoundWhenOfficeBranchDoesNotExist() {
        var officeBranchId = new OfficeBranchId();
        when(officeBranchFinder.find(officeBranchId)).thenReturn(Option.none());

        Either<UseCaseError, List<ServiceResponse>> response = serviceFinder.find(officeBranchId);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST);
    }

    @Test
    void itShouldReturnAllServicesRelatedWithOfficeBranch() {
        var officeBranch = new OfficeBranchBuilder().build();
        var service1 = new ServiceBuilder().build();
        var service2 = new ServiceBuilder().build();
        when(officeBranchFinder.find(officeBranch.id())).thenReturn(Option.of(officeBranch.toResponse()));
        when(serviceRepo.findByOfficeBranch(any())).thenReturn(ImmutableList.of(service1, service2));

        Either<UseCaseError, List<ServiceResponse>> response = serviceFinder.find(officeBranch.id());

        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).size().isEqualTo(2);
        assertThat(response.get()).containsExactlyInAnyOrder(service1.toResponse(), service2.toResponse());
    }
}

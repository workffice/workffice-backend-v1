package search.application;

import backoffice.domain.office.OfficeDeletedEvent;
import io.vavr.control.Option;
import search.domain.OfficeBranchRepository;
import search.factories.OfficeBranchBuilder;
import search.factories.OfficeBuilder;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestOfficeSearchDeleter {
    OfficeBranchRepository officeBranchRepo = mock(OfficeBranchRepository.class);

    OfficeSearchDeleter deleter = new OfficeSearchDeleter(officeBranchRepo);

    @Test
    void itShouldNotUpdateOfficeBranchIfDoesNotExist() {
        when(officeBranchRepo.findById("1")).thenReturn(Option.none());
        var event = OfficeDeletedEvent.of("1", "12");

        deleter.deleteOffice(event);

        verify(officeBranchRepo, times(0)).update(any());
    }

    @Test
    void itShouldUpdateOfficeBranchWithOfficeSpecifiedRemoved() {
        var office1 = new OfficeBuilder().build();
        var office2 = new OfficeBuilder().build();
        var officeBranch = new OfficeBranchBuilder()
                .addOffice(office1)
                .addOffice(office2)
                .build();
        when(officeBranchRepo.findById("1")).thenReturn(Option.of(officeBranch));
        var event = OfficeDeletedEvent.of("1", office1.id());

        deleter.deleteOffice(event);

        officeBranch.removeOffice(office1.id());
        verify(officeBranchRepo, times(1)).update(officeBranch);
    }
}

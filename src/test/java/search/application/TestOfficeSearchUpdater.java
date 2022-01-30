package search.application;

import backoffice.domain.office.OfficeUpdatedEvent;
import io.vavr.control.Option;
import search.domain.Office;
import search.domain.OfficeBranch;
import search.domain.OfficeBranchRepository;
import search.domain.OfficePrivacy;
import search.factories.OfficeBranchBuilder;
import search.factories.OfficeBuilder;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestOfficeSearchUpdater {
    OfficeBranchRepository officeBranchRepo = mock(OfficeBranchRepository.class);
    ArgumentCaptor<OfficeBranch> officeBranchArgumentCaptor = ArgumentCaptor.forClass(OfficeBranch.class);

    OfficeSearchUpdater updater = new OfficeSearchUpdater(officeBranchRepo);

    @Test
    void itShouldNotUpdateOfficeBranchWhenThereIsNoOfficeBranchWithIdSpecifiedInEvent() {
        OfficeUpdatedEvent event = OfficeUpdatedEvent.of(
                "12",
                "1",
                "New name",
                "PRIVATE",
                100,
                500,
                1,
                10
        );
        when(officeBranchRepo.findById("1")).thenReturn(Option.none());

        updater.updateOffice(event);

        verify(officeBranchRepo, times(0)).update(any());
    }

    @Test
    void itShouldUpdateOfficeBranchModifyingSpecifiedOfficeWithInfoProvidedInEvent() {
        var office = OfficeBuilder.builder().build();
        var office2 = OfficeBuilder.builder().build();
        var officeBranch = OfficeBranchBuilder.builder()
                .addOffice(office)
                .addOffice(office2)
                .build();
        OfficeUpdatedEvent event = OfficeUpdatedEvent.of(
                office.id(),
                officeBranch.id(),
                "New name",
                "PRIVATE",
                100,
                500,
                1,
                10
        );
        when(officeBranchRepo.findById(officeBranch.id()))
                .thenReturn(Option.of(officeBranch));

        updater.updateOffice(event);

        verify(officeBranchRepo, times(1)).update(officeBranchArgumentCaptor.capture());
        var officeBranchUpdated = officeBranchArgumentCaptor.getValue();
        assertThat(officeBranchUpdated.offices()).filteredOn(o -> o.id().equals(office.id())).containsExactly(
                Office.create(office.id(), "New name", 100, 500, 1, 10, OfficePrivacy.PRIVATE)
        );
    }
}

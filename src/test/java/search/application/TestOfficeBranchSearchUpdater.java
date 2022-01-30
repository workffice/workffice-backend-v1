package search.application;

import backoffice.domain.office_branch.OfficeBranchUpdatedEvent;
import io.vavr.control.Option;
import search.domain.OfficeBranch;
import search.domain.OfficeBranchRepository;
import search.factories.OfficeBranchBuilder;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestOfficeBranchSearchUpdater {

    OfficeBranchRepository officeBranchRepo = mock(OfficeBranchRepository.class);

    OfficeBranchSearchUpdater updater = new OfficeBranchSearchUpdater(officeBranchRepo);

    @Test
    void itShouldNotUpdateWhenOfficeBranchIdDoesNotExist() {
        var officeBranchId = "1";
        when(officeBranchRepo.findById(officeBranchId)).thenReturn(Option.none());
        OfficeBranchUpdatedEvent event = OfficeBranchUpdatedEvent.of(
                officeBranchId,
                "New name",
                "Mendoza",
                "Godoy Cruz",
                "Fake street 1234",
                "2513749180",
                Arrays.asList("image1.com", "image2.com")
        );

        updater.updateOfficeBranch(event);

        verify(officeBranchRepo, times(0)).update(any());
    }

    @Test
    void itShouldUpdateOfficeBranchWithInfoSpecified() {
        var officeBranch = OfficeBranchBuilder.builder()
                .withId("1")
                .build();
        when(officeBranchRepo.findById("1")).thenReturn(Option.of(officeBranch));
        OfficeBranchUpdatedEvent event = OfficeBranchUpdatedEvent.of(
                "1",
                "New name",
                "Mendoza",
                "Godoy Cruz",
                "Fake street 1234",
                "2513749180",
                Arrays.asList("image1.com", "image2.com")
        );

        updater.updateOfficeBranch(event);

        verify(officeBranchRepo, times(1)).update(OfficeBranch.create(
                "1",
                "New name",
                "2513749180",
                "Mendoza",
                "Godoy Cruz",
                "Fake street 1234",
                Arrays.asList("image1.com", "image2.com")
        ));
    }
}

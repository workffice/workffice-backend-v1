package search.application;

import backoffice.domain.office_branch.OfficeBranchCreatedEvent;
import search.application.dto.OfficeBranchInformation;
import search.domain.OfficeBranch;
import search.domain.OfficeBranchRepository;

import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TestOfficeBranchSearchCreator {
    OfficeBranchRepository officeBranchRepo = mock(OfficeBranchRepository.class);

    OfficeBranchSearchCreator officeBranchSearchCreator = new OfficeBranchSearchCreator(officeBranchRepo);

    @Test
    void itShouldStoreOfficeBranchWithInformationSpecified() {
        var officeBranchInfo = OfficeBranchInformation.of(
                "Monumental",
                "Buenos Aires",
                "Belgrano",
                "Fake street 1234",
                "123456789",
                Arrays.asList("image1.com", "image2.com")
        );
        String id = UUID.randomUUID().toString();

        officeBranchSearchCreator.createOfficeBranch(id, officeBranchInfo);

        var expectedOfficeBranchStored = OfficeBranch.create(
                id,
                "Monumental",
                "123456789",
                "Buenos Aires",
                "Belgrano",
                "Fake street 1234",
                Arrays.asList("image1.com", "image2.com")
        );
        verify(officeBranchRepo, times(1)).store(expectedOfficeBranchStored);
    }

    @Test
    void itShouldStoreOfficeBranchWithInformationSpecifiedByEvent() {
        String id = UUID.randomUUID().toString();
        String ownerId = UUID.randomUUID().toString();
        var event = OfficeBranchCreatedEvent.of(
                id,
                ownerId,
                "Monumental",
                "Buenos Aires",
                "Belgrano",
                "Fake street 1234",
                "123456789",
                Arrays.asList("image1.com", "image2.com")
        );

        officeBranchSearchCreator.createOfficeBranch(event);

        var expectedOfficeBranchStored = OfficeBranch.create(
                id,
                "Monumental",
                "123456789",
                "Buenos Aires",
                "Belgrano",
                "Fake street 1234",
                Arrays.asList("image1.com", "image2.com")
        );
        verify(officeBranchRepo, times(1)).store(expectedOfficeBranchStored);
    }
}

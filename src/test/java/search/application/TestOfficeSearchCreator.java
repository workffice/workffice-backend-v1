package search.application;

import backoffice.domain.office.OfficeCreatedEvent;
import io.vavr.control.Option;
import search.domain.Office;
import search.domain.OfficeBranch;
import search.domain.OfficeBranchRepository;
import search.domain.OfficePrivacy;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestOfficeSearchCreator {
    OfficeBranchRepository officeBranchRepo = mock(OfficeBranchRepository.class);
    ArgumentCaptor<OfficeBranch> officeBranchArgumentCaptor = ArgumentCaptor.forClass(OfficeBranch.class);

    OfficeSearchCreator creator = new OfficeSearchCreator(officeBranchRepo);

    @Test
    void itShouldUpdateOfficeWithInfoSpecifiedByEvent() {
        var event = OfficeCreatedEvent.of(
                "123",
                "1",
                "La scaloneta",
                "PRIVATE",
                100,
                100,
                10,
                10
        );
        var officeBranch = OfficeBranch.create(
                "1",
                "Monumental",
                "2513749180",
                "Buenos Aires",
                "Belgrano",
                "Fake street 1234",
                Arrays.asList("image1.com", "image2.com")
        );
        when(officeBranchRepo.findById("1")).thenReturn(Option.of(officeBranch));

        creator.createOffice(event);

        verify(officeBranchRepo, times(1))
                .update(officeBranchArgumentCaptor.capture());
        var officeBranchUpdated = officeBranchArgumentCaptor.getValue();
        assertThat(officeBranchUpdated.offices()).size().isEqualTo(1);
        assertThat(officeBranchUpdated.offices().get(0)).isEqualTo(
                Office.create(
                        "123",
                        "La scaloneta",
                        100,
                        100,
                        10,
                        10,
                        OfficePrivacy.PRIVATE
                )
        );
    }

    @Test
    void itShouldNotUpdateWhenOfficeBranchDoesNotExist() {
        var event = OfficeCreatedEvent.of(
                "123",
                "1",
                "La scaloneta",
                "PRIVATE",
                100,
                100,
                10,
                10
        );
        when(officeBranchRepo.findById("1")).thenReturn(Option.none());

        creator.createOffice(event);

        verify(officeBranchRepo, times(0)).update(any(OfficeBranch.class));
    }
}

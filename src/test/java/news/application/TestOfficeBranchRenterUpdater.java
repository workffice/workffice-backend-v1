package news.application;

import booking.domain.booking.BookingConfirmedEvent;
import io.vavr.control.Option;
import news.domain.OfficeBranch;
import news.domain.OfficeBranchRepository;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestOfficeBranchRenterUpdater {
    OfficeBranchRepository officeBranchRepo = mock(OfficeBranchRepository.class);
    ArgumentCaptor<OfficeBranch> officeBranchArgumentCaptor = ArgumentCaptor.forClass(OfficeBranch.class);

    OfficeBranchRenterUpdater updater = new OfficeBranchRenterUpdater(officeBranchRepo);

    @Test
    void itShouldStoreOfficeBranchWhenItDoesNotExist() {
        var event = BookingConfirmedEvent.of(
                "1",
                "12",
                "2",
                100f,
                LocalDate.of(2018, 12, 8),
                "pepito@mail.com"
        );
        when(officeBranchRepo.findById("12")).thenReturn(Option.none());

        updater.addRenterEmailToOfficeBranch(event);

        var officeBranchToStore = new OfficeBranch("12");
        officeBranchToStore.addRenterEmail("pepito@mail.com");
        verify(officeBranchRepo, times(1)).store(officeBranchToStore);
    }

    @Test
    void itShouldUpdateOfficeBranchWithNewRenterEmail() {
        var event = BookingConfirmedEvent.of(
                "1",
                "12",
                "2",
                100f,
                LocalDate.of(2018, 12, 8),
                "pepito@mail.com"
        );
        var officeBranch = new OfficeBranch("12");
        officeBranch.addRenterEmail("napoleon@mail.com");
        when(officeBranchRepo.findById("12")).thenReturn(Option.of(officeBranch));

        updater.addRenterEmailToOfficeBranch(event);

        verify(officeBranchRepo, times(1)).update(officeBranchArgumentCaptor.capture());
        var officeBranchUpdated = officeBranchArgumentCaptor.getValue();
        assertThat(officeBranchUpdated.renterEmails())
                .containsExactlyInAnyOrder("pepito@mail.com", "napoleon@mail.com");
    }
}

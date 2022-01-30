package review.application;

import io.vavr.control.Option;
import review.domain.office.Office;
import review.domain.office.OfficeRepository;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestOfficeReviewUpdater {
    OfficeRepository officeRepo = mock(OfficeRepository.class);

    OfficeReviewUpdater updater = new OfficeReviewUpdater(officeRepo);

    @Test
    void itShouldCreateANewOfficeWhenItDoesNotExist() {
        when(officeRepo.findById("12")).thenReturn(Option.none());

        updater.updateOfficeReviews("12", "33", 5);

        verify(officeRepo, times(1)).save(Office.create("12", "33", 5, 1));
    }

    @Test
    void itShouldUpdateExistentOfficeWithNewStars() {
        when(officeRepo.findById("12")).thenReturn(Option.of(Office.create("12", "33", 15, 3)));

        updater.updateOfficeReviews("12", "33", 3);

        verify(officeRepo, times(1)).save(Office.create("12", "33", 18, 4));
    }
}

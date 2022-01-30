package review.domain;

import review.domain.office.Office;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestOffice {
    @Test
    void itShouldReturnVote() {
        var office = Office.create("1", "21", 10, 2);

        var officeUpdated = office.addVote(4);

        assertThat(officeUpdated.starsAverage()).isEqualTo(14 / 3);
    }
}

package news.domain;

import com.github.javafaker.Faker;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestOfficeBranch {

    Faker faker = Faker.instance();

    @Test
    void itShouldAddRenterEmail() {
        var officeBranch = new OfficeBranch("1");

        officeBranch.addRenterEmail("pepito@mail.com");

        assertThat(officeBranch.renterEmails()).containsExactly("pepito@mail.com");
    }

    @Test
    void itShouldNotAddRenterEmailWhenAlreadyExists() {
        var officeBranch = new OfficeBranch("1");
        officeBranch.addRenterEmail("pepito@mail.com");

        officeBranch.addRenterEmail("pepito@mail.com");

        assertThat(officeBranch.renterEmails()).containsExactly("pepito@mail.com");
    }

    @Test
    void itShouldRemoveFirstRenterEmailWhenRenterEmailsIsGreaterThan30() {
        var officeBranch = new OfficeBranch("1");
        officeBranch.addRenterEmail("pepito@mail.com");
        IntStream.range(0, 29).forEach(c -> officeBranch.addRenterEmail(faker.internet().emailAddress()));
        assertThat(officeBranch.renterEmails()).containsOnlyOnce("pepito@mail.com");

        officeBranch.addRenterEmail("napoleon@mail.com");

        assertThat(officeBranch.renterEmails()).doesNotContain("pepito@mail.com");
        assertThat(officeBranch.renterEmails()).containsOnlyOnce("napoleon@mail.com");
    }
}

package booking.domain.office.privacy;

import io.vavr.control.Try;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestPrivacy {

    @Test
    void itShouldReturnFailureWhenTryingToCreateAnInvalidPrivacyType() {
        Try<Privacy> privacy = Privacy.createPrivacy("INVALId", 10, 1, 10);

        assertThat(privacy.isFailure()).isTrue();
    }

    @Test
    void itShouldCreateSharedPrivacyWhenPrivacyIsShared() {
        Try<Privacy> privacy = Privacy.createPrivacy("SHARED", 10, 1, 10);

        assertThat(privacy.isSuccess()).isTrue();
        assertThat(privacy.get()).isInstanceOf(SharedOffice.class);
    }

    @Test
    void itShouldCreatePrivatePrivacyWhenPrivacyIsPrivate() {
        Try<Privacy> privacy = Privacy.createPrivacy("PRIVATE", 10, 1, 10);

        assertThat(privacy.isSuccess()).isTrue();
        assertThat(privacy.get()).isInstanceOf(PrivateOffice.class);
    }
}

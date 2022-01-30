package authentication.domain;

import authentication.domain.user.AuthUserId;

import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestAuthUserId {

    @Test
    public void itShouldReturnUUIDString() {
        UUID uuid = UUID.randomUUID();
        AuthUserId id = new AuthUserId(uuid);

        Assertions.assertThat(id.toString()).isEqualTo(uuid.toString());
    }
}

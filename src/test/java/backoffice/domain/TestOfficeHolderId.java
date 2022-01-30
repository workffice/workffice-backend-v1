package backoffice.domain;

import backoffice.domain.office_holder.OfficeHolderId;

import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestOfficeHolderId {

    @Test
    public void testItShouldReturnUUIDStringEquivalent() {
        UUID uuid = UUID.randomUUID();
        OfficeHolderId id = new OfficeHolderId(uuid);

        assertThat(id.toString()).isEqualTo(uuid.toString());
    }
}

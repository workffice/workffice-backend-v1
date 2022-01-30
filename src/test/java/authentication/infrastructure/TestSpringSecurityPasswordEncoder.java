package authentication.infrastructure;

import authentication.infrastructure.springsecurity.SpringSecurityPasswordEncoder;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestSpringSecurityPasswordEncoder {

    PasswordEncoder mockPasswordEncoder = mock(PasswordEncoder.class);
    SpringSecurityPasswordEncoder passwordEncoder = new SpringSecurityPasswordEncoder(mockPasswordEncoder);

    @Test
    public void itShouldEncodeUsingSpringSecurityPasswordEncoder() {
        when(mockPasswordEncoder.encode(anyString())).thenReturn("Super Secret");

        String encodedString = passwordEncoder.encode("1234");

        assertThat(encodedString).isEqualTo("Super Secret");
    }
}

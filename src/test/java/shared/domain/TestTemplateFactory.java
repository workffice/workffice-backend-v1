package shared.domain;

import shared.domain.email.template.Template;
import shared.domain.email.template.TemplateFactory;
import shared.infrastructure.config.EnvironmentConfigurationError;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestTemplateFactory {
    Environment env = mock(Environment.class);

    @Test
    void itShouldRaiseEnvironmentConfigurationErrorWhenInstantiateTemplateFactoryAndClientHostIsNull() {
        when(env.getProperty("CLIENT_HOST")).thenReturn(null);

        assertThatThrownBy(() -> new TemplateFactory(env))
                .isInstanceOf(EnvironmentConfigurationError.class);
    }

    @Test
    void itShouldReturnConfirmationTemplateWithUrlUsingHostProvidedByEnvVariable()
            throws EnvironmentConfigurationError {
        when(env.getProperty("CLIENT_HOST")).thenReturn("http://localhost:3000");
        TemplateFactory templateFactory = new TemplateFactory(env);

        Template confirmationTemplate = templateFactory.createAccountConfirmationTemplate("1234");

        assertThat(confirmationTemplate.substitutionData().get("url"))
                .startsWith("http://localhost:3000");
    }

    @Test
    void itShouldReturnPasswordResetTemplateWithUrlUsingHostProvidedByEnvVariable()
            throws EnvironmentConfigurationError {
        when(env.getProperty("CLIENT_HOST")).thenReturn("http://localhost:3000");
        TemplateFactory templateFactory = new TemplateFactory(env);

        Template passwordResetTemplate = templateFactory.createPasswordResetTemplate("1234");

        assertThat(passwordResetTemplate.substitutionData().get("url"))
                .startsWith("http://localhost:3000");
    }
}

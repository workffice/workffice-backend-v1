package shared.infrastructure.config;

import com.sendgrid.SendGrid;

import java.util.HashMap;
import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class SpringEmailConfig {
    private final Environment env;
    
    public SpringEmailConfig(Environment env) throws EnvironmentConfigurationError {
        this.env = env;
        verifyEnvVariables();
    }
    
    private void verifyEnvVariables() throws EnvironmentConfigurationError {
        HashMap<String, String> variables = new HashMap<>() {{
            put("SENDGRID_API_KEY", env.getProperty("SENDGRID_API_KEY"));
            put("EMAIL_USERNAME", env.getProperty("EMAIL_USERNAME"));
            put("EMAIL_PASSWORD", env.getProperty("EMAIL_PASSWORD"));
        }};
        ConfigUtil.verifyEnvVariables(variables);
    }
    
    @Bean
    public SendGrid sendGridClient() {
        return new SendGrid(env.getProperty("SENDGRID_API_KEY", ""));
    }
    
    @Bean
    @Primary
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.sendgrid.net");
        mailSender.setPort(587);
        mailSender.setUsername(env.getProperty("EMAIL_USERNAME"));
        mailSender.setPassword(env.getProperty("EMAIL_PASSWORD"));
        mailSender.setJavaMailProperties(getMailProperties());
        return mailSender;
    }
    
    private Properties getMailProperties() {
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");
        return props;
    }
    
}

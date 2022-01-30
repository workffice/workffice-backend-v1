package shared.infrastructure.config;

import java.util.HashMap;
import java.util.Properties;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class AuthenticationJPAConfig {

    private final Environment env;

    public AuthenticationJPAConfig(Environment env) throws EnvironmentConfigurationError {
        this.env = env;
        verifyEnvVariables();
    }

    @Bean
    @Qualifier("authenticationEntityManagerFactory")
    public LocalSessionFactoryBean authenticationSessionFactoryBean() {
        LocalSessionFactoryBean sessionFactoryBean = new LocalSessionFactoryBean();
        sessionFactoryBean.setDataSource(authenticationDataSource());
        sessionFactoryBean.setPackagesToScan("authentication.domain");
        sessionFactoryBean.setHibernateProperties(authenticationHibernateProperties());
        return sessionFactoryBean;
    }

    private void verifyEnvVariables() throws EnvironmentConfigurationError {
        HashMap<String, String> envVariables = new HashMap<>() {{
            put("db.authentication_username", env.getProperty("db.authentication_username"));
            put("db.authentication_password", env.getProperty("db.authentication_password"));
            put("db.authentication_host", env.getProperty("db.authentication_host"));
            put("db.driver", env.getProperty("db.driver"));
            put("db.dialect", env.getProperty("db.dialect"));
            put("db.ddl", env.getProperty("db.ddl"));
        }};
        ConfigUtil.verifyEnvVariables(envVariables);
    }

    @Bean
    public DataSource authenticationDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(env.getProperty("db.driver"));
        dataSource.setUsername(env.getProperty("db.authentication_username"));
        dataSource.setPassword(env.getProperty("db.authentication_password"));
        dataSource.setUrl(env.getProperty("db.authentication_host"));
        return dataSource;
    }

    @Bean
    public PlatformTransactionManager authenticationTransactionManager() {
        HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        transactionManager.setSessionFactory(authenticationSessionFactoryBean().getObject());
        return transactionManager;
    }

    @Bean
    public Properties authenticationHibernateProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", env.getProperty("db.ddl"));
        properties.setProperty("hibernate.dialect", env.getProperty("db.dialect"));
        properties.setProperty("hibernate.show_sql", env.getProperty("db.show_sql", "false"));
        return properties;
    }

}


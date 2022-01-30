package shared.infrastructure.config;

import java.util.HashMap;
import java.util.Properties;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class BackofficeJPAConfig {

    private final Environment env;

    public BackofficeJPAConfig(Environment env) throws EnvironmentConfigurationError {
        this.env = env;
        verifyEnvVariables();
    }

    @Bean
    @Primary
    public LocalSessionFactoryBean sessionFactoryBean() {
        LocalSessionFactoryBean sessionFactoryBean = new LocalSessionFactoryBean();
        sessionFactoryBean.setDataSource(dataSource());
        sessionFactoryBean.setPackagesToScan("backoffice.domain");
        sessionFactoryBean.setHibernateProperties(hibernateProperties());
        return sessionFactoryBean;
    }

    private void verifyEnvVariables() throws EnvironmentConfigurationError {
        HashMap<String, String> envVariables = new HashMap<>() {{
            put("db.backoffice_username", env.getProperty("db.backoffice_username"));
            put("db.backoffice_password", env.getProperty("db.backoffice_password"));
            put("db.backoffice_host", env.getProperty("db.backoffice_host"));
            put("db.driver", env.getProperty("db.driver"));
            put("db.dialect", env.getProperty("db.dialect"));
            put("db.ddl", env.getProperty("db.ddl"));
        }};
        ConfigUtil.verifyEnvVariables(envVariables);
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(env.getProperty("db.driver"));
        dataSource.setUsername(env.getProperty("db.backoffice_username"));
        dataSource.setPassword(env.getProperty("db.backoffice_password"));
        dataSource.setUrl(env.getProperty("db.backoffice_host"));
        return dataSource;
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager() {
        HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        transactionManager.setSessionFactory(sessionFactoryBean().getObject());
        return transactionManager;
    }

    @Bean
    @Primary
    public Properties hibernateProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", env.getProperty("db.ddl"));
        properties.setProperty("hibernate.dialect", env.getProperty("db.dialect"));
        properties.setProperty("hibernate.show_sql", env.getProperty("db.show_sql", "false"));
        return properties;
    }
}

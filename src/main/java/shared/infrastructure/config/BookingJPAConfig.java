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
public class BookingJPAConfig {
    private final Environment env;

    public BookingJPAConfig(Environment env) throws EnvironmentConfigurationError {
        this.env = env;
        verifyEnvVariables();
    }

    @Bean
    @Qualifier("bookingEntityManagerFactory")
    public LocalSessionFactoryBean bookingSessionFactoryBean() {
        LocalSessionFactoryBean sessionFactoryBean = new LocalSessionFactoryBean();
        sessionFactoryBean.setDataSource(bookingDataSource());
        sessionFactoryBean.setPackagesToScan("booking.domain");
        sessionFactoryBean.setHibernateProperties(bookingHibernateProperties());
        return sessionFactoryBean;
    }

    private void verifyEnvVariables() throws EnvironmentConfigurationError {
        HashMap<String, String> envVariables = new HashMap<>() {{
            put("db.booking_username", env.getProperty("db.booking_username"));
            put("db.booking_password", env.getProperty("db.booking_password"));
            put("db.booking_host", env.getProperty("db.backoffice_host"));
            put("db.driver", env.getProperty("db.driver"));
            put("db.dialect", env.getProperty("db.dialect"));
            put("db.ddl", env.getProperty("db.ddl"));
        }};
        ConfigUtil.verifyEnvVariables(envVariables);
    }

    @Bean
    public DataSource bookingDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(env.getProperty("db.driver"));
        dataSource.setUsername(env.getProperty("db.booking_username"));
        dataSource.setPassword(env.getProperty("db.booking_password"));
        dataSource.setUrl(env.getProperty("db.booking_host"));
        return dataSource;
    }

    @Bean
    public PlatformTransactionManager bookingTransactionManager() {
        HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        transactionManager.setSessionFactory(bookingSessionFactoryBean().getObject());
        return transactionManager;
    }

    @Bean
    public Properties bookingHibernateProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", env.getProperty("db.ddl"));
        properties.setProperty("hibernate.dialect", env.getProperty("db.dialect"));
        properties.setProperty("hibernate.show_sql", env.getProperty("db.show_sql", "false"));
        return properties;
    }
}

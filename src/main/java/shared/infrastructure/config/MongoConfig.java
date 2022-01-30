package shared.infrastructure.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import java.util.HashMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoConfig {

    private final Environment env;
    private final String MONGO_HOST;
    private final String MONGO_DB;

    public MongoConfig(Environment env) throws EnvironmentConfigurationError {
        this.env = env;
        ConfigUtil.verifyEnvVariables(new HashMap<>() {{
            put("mongo.host", env.getProperty("mongo.host"));
            put("mongo.db", env.getProperty("mongo.db"));
        }});
        this.MONGO_HOST = env.getProperty("mongo.host");
        this.MONGO_DB = env.getProperty("mongo.db");
    }

    @Bean
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(MONGO_HOST);
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();
        return MongoClients.create(mongoClientSettings);
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoClient(), MONGO_DB);
    }
}

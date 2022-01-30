package news.factory;

import com.github.javafaker.Faker;
import news.domain.News;

import java.util.UUID;

public class NewsBuilder {

    private Faker faker = Faker.instance();
    private String id = UUID.randomUUID().toString();
    private String officeBranchId = UUID.randomUUID().toString();
    private String subject = faker.lorem().characters();
    private String title = faker.lorem().characters();
    private String body = faker.lorem().characters();

    public NewsBuilder withOfficeBranchId(String officeBranchId) {
        this.officeBranchId = officeBranchId;
        return this;
    }

    public News build() {
        return News.create(
                id,
                officeBranchId,
                subject,
                title,
                body
        );
    }
}

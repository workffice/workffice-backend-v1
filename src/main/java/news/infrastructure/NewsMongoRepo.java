package news.infrastructure;

import io.vavr.control.Option;
import io.vavr.control.Try;
import news.domain.News;
import news.domain.NewsRepository;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class NewsMongoRepo implements NewsRepository {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Try<Void> store(News news) {
        return Try.run(() -> mongoTemplate.save(news));
    }

    @Override
    public Try<Void> update(News news) {
        return Try.run(() -> mongoTemplate.save(news));
    }

    @Override
    public Option<News> findById(String id) {
        return Option.of(mongoTemplate.findById(id, News.class));
    }

    @Override
    public List<News> findByOfficeBranch(String officeBranchId) {
        var criteria = Criteria.where("officeBranchId").is(officeBranchId);
        var query = Query.query(criteria);
        return mongoTemplate.find(query, News.class);
    }
}

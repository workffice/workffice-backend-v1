package review.infrastructure;

import io.vavr.control.Option;
import io.vavr.control.Try;
import review.domain.office.Office;
import review.domain.office.OfficeRepository;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class OfficeMongoRepo implements OfficeRepository {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Try<Void> save(Office office) {
        return Try.run(() -> mongoTemplate.save(office));
    }

    @Override
    public Option<Office> findById(String id) {
        var criteria = Criteria.where("id").is(id);
        return Option.of(mongoTemplate.findOne(Query.query(criteria), Office.class));
    }

    @Override
    public List<Office> findByOfficeBranchId(String officeBranchId) {
        var criteria = Criteria.where("officeBranchId").is(officeBranchId);
        return mongoTemplate.find(Query.query(criteria), Office.class);
    }
}

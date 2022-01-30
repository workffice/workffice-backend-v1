package search.infrastructure;

import io.vavr.control.Option;
import search.domain.OfficeBranch;
import search.domain.OfficeBranchRepository;
import search.domain.spec.Specification;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class OfficeBranchMongoRepo implements OfficeBranchRepository {

    private final MongoTemplate mongoTemplate;

    public OfficeBranchMongoRepo(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void store(OfficeBranch officeBranch) {
        mongoTemplate.save(officeBranch);
    }

    @Override
    public void update(OfficeBranch officeBranch) { mongoTemplate.save(officeBranch); }

    @Override
    public void delete(String officeBranchId) {
        var criteria = Criteria.where("id").is(officeBranchId);
        var query = Query.query(criteria);
        mongoTemplate.remove(query, OfficeBranch.class);
    }

    @Override
    public Option<OfficeBranch> findById(String id) {
        var officeBranch = mongoTemplate.findById(id, OfficeBranch.class);
        return Option.of(officeBranch);
    }

    @Override
    public List<OfficeBranch> search(Specification spec) {
        var query = MongoCriteriaAdapter.obtainMongoQuery(spec);
        return mongoTemplate.find(query, OfficeBranch.class);
    }

    @Override
    public List<OfficeBranch> search(List<Specification> specs, Integer offset, Integer limit) {
        var query = MongoCriteriaAdapter.obtainMongoQuery(specs, offset, limit);

        return query
                .map(q -> mongoTemplate.find(q, OfficeBranch.class))
                .getOrElse(mongoTemplate.query(OfficeBranch.class)
                        .stream()
                        .skip(offset)
                        .limit(limit)
                        .collect(Collectors.toList()));
    }

    @Override
    public Long count(List<Specification> specs) {
        var query = MongoCriteriaAdapter.obtainMongoQuery(specs);
        return query
                .map(q -> mongoTemplate.count(q, OfficeBranch.class))
                .getOrElse(mongoTemplate.estimatedCount(OfficeBranch.class));
    }
}

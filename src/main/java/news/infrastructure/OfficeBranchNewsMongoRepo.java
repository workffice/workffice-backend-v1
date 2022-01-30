package news.infrastructure;

import io.vavr.control.Option;
import news.domain.OfficeBranch;
import news.domain.OfficeBranchRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OfficeBranchNewsMongoRepo implements OfficeBranchRepository {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void store(OfficeBranch officeBranch) {
        mongoTemplate.save(officeBranch);
    }

    @Override
    public void update(OfficeBranch officeBranch) {
        mongoTemplate.save(officeBranch);
    }

    @Override
    public Option<OfficeBranch> findById(String officeBranchId) {
        return Option.of(mongoTemplate.findById(officeBranchId, OfficeBranch.class));
    }
}

package backoffice.infrastructure;

import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_holder.OfficeHolder;
import backoffice.domain.office_holder.OfficeHolderId;
import backoffice.domain.office_holder.OfficeHolderRepository;
import io.vavr.control.Option;

import javax.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class OfficeHolderMySQLRepo
        extends BackofficeJPARepo<OfficeHolder, OfficeHolderId> implements OfficeHolderRepository {
    @Override
    public void store(OfficeHolder officeHolder) {
        this.save(officeHolder);
    }

    @Override
    public Option<OfficeHolder> findById(OfficeHolderId id) {
        return super.findById(id);
    }

    @Override
    public Option<OfficeHolder> findByOfficeBranch(OfficeBranch officeBranch) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Option<OfficeHolder> officeHolder = Option
                .of(entityManager.find(OfficeBranch.class, officeBranch.id()))
                .map(officeBranchSaved -> entityManager.find(OfficeHolder.class, officeBranchSaved.owner().id()));
        entityManager.close();
        return officeHolder;
    }

    @Override
    public Option<OfficeHolder> find(String email) {
        return findByColumn("email", email);
    }

    @Override
    public Class<OfficeHolder> getEntityClass() {
        return OfficeHolder.class;
    }
}

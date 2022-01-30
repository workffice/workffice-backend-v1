package backoffice.infrastructure;

import backoffice.domain.equipment.Equipment;
import backoffice.domain.equipment.EquipmentId;
import backoffice.domain.equipment.EquipmentRepository;
import backoffice.domain.office_branch.OfficeBranch;
import io.vavr.Function3;
import io.vavr.control.Try;

import java.util.List;
import java.util.function.Consumer;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

@Repository
public class EquipmentMySQLRepo extends BackofficeJPARepo<Equipment, EquipmentId> implements EquipmentRepository {
    @Override
    public Try<Void> store(Equipment equipment) {
        return save(equipment);
    }

    @Override
    public List<Equipment> findByOfficeBranch(OfficeBranch officeBranch) {
        var entityManager = entityManagerFactory.createEntityManager();
        Function3<
                CriteriaQuery<Equipment>,
                Root<Equipment>,
                CriteriaBuilder,
                CriteriaQuery<Equipment>
                > addConstraints = (query, table, builder) -> query.where(builder.equal(table.get("officeBranch"),
                officeBranch));
        Consumer<Root<Equipment>> join = table -> {
        };
        List<Equipment> equipments = findAll(entityManager, addConstraints, join, getEntityClass());
        entityManager.close();
        return equipments;
    }


    @Override
    public Class<Equipment> getEntityClass() {
        return Equipment.class;
    }
}

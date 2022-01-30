package backoffice.domain.equipment;

import backoffice.domain.office_branch.OfficeBranch;
import io.vavr.control.Try;

import java.util.List;

public interface EquipmentRepository {

    Try<Void> store(Equipment equipment);

    List<Equipment> findByOfficeBranch(OfficeBranch officeBranch);

}

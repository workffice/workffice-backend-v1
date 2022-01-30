package review.domain.office;

import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.List;

public interface OfficeRepository {

    Try<Void> save(Office office);

    Option<Office> findById(String id);

    List<Office> findByOfficeBranchId(String officeBranchId);
}

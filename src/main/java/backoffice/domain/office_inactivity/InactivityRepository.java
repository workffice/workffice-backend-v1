package backoffice.domain.office_inactivity;

import backoffice.domain.office.Office;
import io.vavr.control.Try;

import java.util.List;

public interface InactivityRepository {

    Try<Void> store(Inactivity inactivity);

    Try<Void> bulkStore(List<Inactivity> inactivities);

    List<Inactivity> findAllByOffice(Office office);

    Try<Void> delete(List<Inactivity> inactivities);
}

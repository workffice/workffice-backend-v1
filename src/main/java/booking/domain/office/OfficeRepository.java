package booking.domain.office;

import io.vavr.control.Option;
import io.vavr.control.Try;

public interface OfficeRepository {

    Try<Void> store(Office office);

    Try<Void> update(Office office);

    Option<Office> findById(OfficeId id);
}

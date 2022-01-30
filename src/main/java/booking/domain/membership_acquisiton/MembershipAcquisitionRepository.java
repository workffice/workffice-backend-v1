package booking.domain.membership_acquisiton;

import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.List;

public interface MembershipAcquisitionRepository {
    Try<Void> store(MembershipAcquisition membershipAcquisition);

    Try<Void> update(MembershipAcquisition membershipAcquisition);

    Option<MembershipAcquisition> findById(MembershipAcquisitionId id);

    List<MembershipAcquisition> find(String buyerEmail);
}

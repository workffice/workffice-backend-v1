package booking.domain.payment_preference;

import booking.application.dto.booking.BookingError;
import booking.application.dto.booking.BookingPreferenceInformation;
import booking.application.dto.membership_acquisition.MembershipAcquisitionError;
import booking.application.dto.membership_acquisition.MembershipAcquisitionPreference;
import io.vavr.control.Either;

public interface PreferenceCreator {
    Either<BookingError, PaymentPreference> createForBooking(BookingPreferenceInformation info);

    Either<MembershipAcquisitionError, PaymentPreference> createForMembership(MembershipAcquisitionPreference info);
}

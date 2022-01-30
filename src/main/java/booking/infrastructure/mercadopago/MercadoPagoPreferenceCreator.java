package booking.infrastructure.mercadopago;

import booking.application.dto.booking.BookingError;
import booking.application.dto.booking.BookingPreferenceInformation;
import booking.application.dto.membership_acquisition.MembershipAcquisitionError;
import booking.application.dto.membership_acquisition.MembershipAcquisitionPreference;
import booking.domain.payment_preference.PaymentPreference;
import booking.domain.payment_preference.PreferenceCreator;
import com.mercadopago.MercadoPago;
import com.mercadopago.exceptions.MPConfException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.Preference;
import com.mercadopago.resources.datastructures.preference.BackUrls;
import com.mercadopago.resources.datastructures.preference.Item;
import com.mercadopago.resources.datastructures.preference.Payer;
import com.mercadopago.resources.datastructures.preference.PaymentMethods;
import io.vavr.control.Either;

import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Component
public class MercadoPagoPreferenceCreator implements PreferenceCreator {
    private final Environment env;
    private Preference preference = null;

    public MercadoPagoPreferenceCreator(Environment env) {
        this.env = env;
    }

    public void setPreference(Preference preference) {
        // Only for testing
        this.preference = preference;
    }

    @Override
    public Either<BookingError, PaymentPreference> createForBooking(BookingPreferenceInformation info) {
        try {
            MercadoPago.SDK.setAccessToken(env.getProperty("MERCADO_PAGO_ACCESS_TOKEN"));
        } catch (MPConfException e) {
            LoggerFactory.getLogger(getClass()).error(e.toString());
            return Either.left(BookingError.MERCADO_PAGO_ERROR);
        }
        PaymentMethods paymentMethods = new PaymentMethods();
        paymentMethods.setExcludedPaymentTypes("ticket", "atm");

        Item item = new Item();
        item.setId(info.getBookingId())
                .setTitle(info.getOfficeName())
                .setQuantity(1)
                .setCategoryId("ARS")
                .setDescription(info.getDescription())
                .setUnitPrice(info.getPrice());

        Payer payer = new Payer();
        payer.setEmail(info.getRenterEmail());

        var newPreference = preference != null ? preference : new Preference();
        /* preference.setBinaryMode(true); Only accept reject or approved status
        https://www.mercadopago.com.ar/developers/es/guides/online-payments/checkout-pro/configurations */
        /* Set an expiration date in order to not receive payments
        for a booking that is no longer pending
        preference.setExpirationDateTo() */
        newPreference.setStatementDescriptor("Alquiler Workffice APP");
        newPreference.setBackUrls(new BackUrls(
                format("%s/admin/booking?id=%s", env.getProperty("CLIENT_HOST"), info.getBookingId()),
                "",
                ""
        ));
        newPreference.setPaymentMethods(paymentMethods);
        newPreference.setPayer(payer);
        newPreference.appendItem(item);
        newPreference.setNotificationUrl(format(
                "%s/api/bookings/%s/mp_notifications/?source_news=webhooks",
                env.getProperty("SERVER_HOST"),
                info.getBookingId()
        ));
        try {
            newPreference.save();
            LoggerFactory.getLogger(getClass()).info(newPreference.getLastApiResponse().getStringResponse());
            return Either.right(PaymentPreference.of(newPreference.getId()));
        } catch (MPException e) {
            LoggerFactory.getLogger(getClass()).error(e.toString());
            return Either.left(BookingError.MERCADO_PAGO_ERROR);
        }
    }

    @Override
    public Either<MembershipAcquisitionError, PaymentPreference> createForMembership(
            MembershipAcquisitionPreference info
    ) {
        try {
            MercadoPago.SDK.setAccessToken(env.getProperty("MERCADO_PAGO_ACCESS_TOKEN"));
        } catch (MPConfException e) {
            LoggerFactory.getLogger(getClass()).error(e.toString());
            return Either.left(MembershipAcquisitionError.MERCADO_PAGO_ERROR);
        }
        PaymentMethods paymentMethods = new PaymentMethods();
        paymentMethods.setExcludedPaymentTypes("ticket", "atm");

        Item item = new Item();
        item.setId(info.getMembershipAcquisitionId())
                .setTitle(info.getMembershipName())
                .setQuantity(1)
                .setCategoryId("ARS")
                .setDescription(info.getDescription())
                .setUnitPrice(info.getPrice());

        Payer payer = new Payer();
        payer.setEmail(info.getBuyerEmail());

        var newPreference = preference != null ? preference : new Preference();
        /* preference.setBinaryMode(true); Only accept reject or approved status
        https://www.mercadopago.com.ar/developers/es/guides/online-payments/checkout-pro/configurations */
        /* Set an expiration date in order to not receive payments
        for a booking that is no longer pending
        preference.setExpirationDateTo() */
        newPreference.setStatementDescriptor("Alquiler Workffice APP");
        newPreference.setBackUrls(new BackUrls(
                format(
                        "%s/admin/membership_acquisitions?id=%s",
                        env.getProperty("CLIENT_HOST"),
                        info.getMembershipAcquisitionId()
                ),
                "",
                ""
        ));
        newPreference.setPaymentMethods(paymentMethods);
        newPreference.setPayer(payer);
        newPreference.appendItem(item);
        newPreference.setNotificationUrl(format(
                "%s/api/membership_acquisitions/%s/mp_notifications/?source_news=webhooks",
                env.getProperty("SERVER_HOST"),
                info.getMembershipAcquisitionId()
        ));
        try {
            newPreference.save();
            LoggerFactory.getLogger(getClass()).info(newPreference.getLastApiResponse().getStringResponse());
            return Either.right(PaymentPreference.of(newPreference.getId()));
        } catch (MPException e) {
            LoggerFactory.getLogger(getClass()).error(e.toString());
            return Either.left(MembershipAcquisitionError.MERCADO_PAGO_ERROR);
        }
    }
}

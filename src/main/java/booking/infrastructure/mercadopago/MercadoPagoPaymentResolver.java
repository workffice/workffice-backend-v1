package booking.infrastructure.mercadopago;

import backoffice.application.dto.office_branch.OfficeBranchResponse;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.office_branch.OfficeBranchId;
import booking.application.booking.BookingEmailNotificator;
import booking.domain.booking.Booking;
import booking.domain.booking.BookingId;
import booking.domain.booking.BookingRepository;
import booking.domain.booking.PaymentInformation;
import booking.domain.membership_acquisiton.MembershipAcquisition;
import booking.domain.membership_acquisiton.MembershipAcquisitionId;
import booking.domain.membership_acquisiton.MembershipAcquisitionRepository;
import com.google.common.collect.ImmutableList;
import com.mercadopago.MercadoPago;
import com.mercadopago.exceptions.MPConfException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.Payment;
import com.mercadopago.resources.datastructures.payment.FeeDetail;
import io.vavr.control.Option;
import shared.domain.EventBus;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class MercadoPagoPaymentResolver {
    private final Environment                     env;
    private final Logger                          logger;
    private final BookingRepository               bookingRepo;
    private final MembershipAcquisitionRepository membershipAcquisitionRepo;
    private final BookingEmailNotificator         emailNotificator;
    private final OfficeBranchFinder              officeBranchFinder;
    private final EventBus                        eventBus;

    private final List<Payment.Status> failedPaymentStatuses = ImmutableList.of(
            Payment.Status.cancelled,
            Payment.Status.rejected,
            Payment.Status.refunded,
            Payment.Status.charged_back
    );

    public MercadoPagoPaymentResolver(
            Environment                     env,
            BookingRepository               bookingRepo,
            MembershipAcquisitionRepository membershipAcquisitionRepo,
            BookingEmailNotificator         emailNotificator,
            OfficeBranchFinder              officeBranchFinder,
            EventBus                        eventBus
    ) {
        this.env                       = env;
        this.logger                    = LoggerFactory.getLogger(getClass());
        this.bookingRepo               = bookingRepo;
        this.membershipAcquisitionRepo = membershipAcquisitionRepo;
        this.emailNotificator          = emailNotificator;
        this.officeBranchFinder        = officeBranchFinder;
        this.eventBus                  = eventBus;
    }

    private Option<Payment> obtainPayment(MercadoPagoPaymentNotification notification) {
        try {
            MercadoPago.SDK.setAccessToken(env.getProperty("MERCADO_PAGO_ACCESS_TOKEN"));
        } catch (MPConfException e) {
            logger.error(e.toString());
            return Option.none();
        }

        try {
            Payment payment = Payment.findById(notification.getData().getId());
            return Option.of(payment);
        } catch (MPException e) {
            logger.error(e.toString());
            return Option.none();
        }
    }

    private void handlePaymentAccepted(Booking booking, Payment payment) {
        var paymentInformation = new PaymentInformation(
                payment.getId(),
                payment.getTransactionAmount(),
                payment.getFeeDetails()
                        .stream()
                        .filter(fee -> fee.getType().equals(FeeDetail.Type.mercadopago_fee))
                        .map(FeeDetail::getAmount).findFirst().orElse(0f),
                payment.getCurrencyId().name(),
                payment.getPaymentMethodId(),
                payment.getPaymentTypeId().name()
        );
        booking.markAsScheduled(paymentInformation);
        bookingRepo.update(booking)
                .onSuccess(v -> {
                    var officeBranchLocation = officeBranchFinder
                            .find(OfficeBranchId.fromString(booking.office().officeBranchId()))
                            .map(OfficeBranchResponse::getLocation)
                            .getOrElse(new OfficeBranchResponse.Location("N/A", "", "", ""));
                    eventBus.publish(booking.bookingConfirmedEvent());
                    emailNotificator.sendBookingPaymentAcceptedEmail(
                            booking.renterEmail(),
                            booking.id(),
                            booking.office().name(),
                            booking.startScheduleTime(),
                            booking.endScheduleTime(),
                            booking.amountOfHours(),
                            payment.getTransactionAmount(),
                            officeBranchLocation
                    );
                }).onFailure(error -> logger.error(error.toString()));
    }

    private void updateBooking(BookingId bookingId, Payment payment) {
        bookingRepo.findById(bookingId)
                .filter(Booking::isPending)
                .peek(booking -> {
                    if (payment.getStatus().equals(Payment.Status.approved)) {
                        handlePaymentAccepted(booking, payment);
                    } else if (failedPaymentStatuses.contains(payment.getStatus()))
                        emailNotificator.sendBookingPaymentFailedEmail(booking.renterEmail());
                });
    }

    public void handleNotification(
            String bookingId,
            MercadoPagoPaymentNotification notification
    ) {
        try {
            var id = BookingId.fromString(bookingId);
            obtainPayment(notification)
                    .peek(payment -> updateBooking(id, payment));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid booking id");
        }
    }

    public void handleNotificationForMembershipAcquisition(
            String membershipAcquisitionId,
            MercadoPagoPaymentNotification notification
    ) {
        try {
            var id = MembershipAcquisitionId.fromString(membershipAcquisitionId);
            obtainPayment(notification)
                    .peek(payment -> updateMembershipAcquisition(id, payment));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid membership acquisition id");
        }
    }

    private void updateMembershipAcquisition(MembershipAcquisitionId id, Payment payment) {
        membershipAcquisitionRepo.findById(id)
                .filter(MembershipAcquisition::isPending)
                .peek(membershipAcquisition -> {
                    if (payment.getStatus().equals(Payment.Status.approved)) {
                        var paymentInformation = new PaymentInformation(
                                payment.getId(),
                                payment.getTransactionAmount(),
                                payment.getFeeDetails()
                                        .stream()
                                        .filter(fee -> fee.getType().equals(FeeDetail.Type.mercadopago_fee))
                                        .map(FeeDetail::getAmount).findFirst().orElse(0f),
                                payment.getCurrencyId().name(),
                                payment.getPaymentMethodId(),
                                payment.getPaymentTypeId().name()
                        );
                        membershipAcquisition.buy(paymentInformation);
                        membershipAcquisitionRepo.update(membershipAcquisition);
                    } else if (failedPaymentStatuses.contains(payment.getStatus()))
                        logger.error("Payment was rejected");
                });
    }
}

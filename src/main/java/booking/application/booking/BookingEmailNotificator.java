package booking.application.booking;

import backoffice.application.dto.office_branch.OfficeBranchResponse;
import booking.domain.booking.BookingId;
import shared.domain.email.EmailSender;
import shared.domain.email.Message;
import shared.domain.email.template.TemplateFactory;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class BookingEmailNotificator {

    private final EmailSender emailSender;
    private final TemplateFactory templateFactory;

    public BookingEmailNotificator(EmailSender emailSender, TemplateFactory templateFactory) {
        this.emailSender = emailSender;
        this.templateFactory = templateFactory;
    }

    public void sendBookingPaymentFailedEmail(String recipient) {
        emailSender.send(Message.builder()
                .recipient(recipient)
                .template(templateFactory.createBookingPaymentFailedTemplate())
                .build()
        );
    }

    public void sendBookingPaymentAcceptedEmail(
            String        recipient,
            BookingId     bookingId,
            String        officeName,
            LocalDateTime startScheduleTime,
            LocalDateTime endScheduleTime,
            Integer       bookingHoursQuantity,
            Float         totalPrice,
            OfficeBranchResponse.Location officeBranchLocation
    ) {
        emailSender.send(Message.builder()
                .recipient(recipient)
                .template(templateFactory.createBookingPaymentAcceptedTemplate(
                        bookingId.toString(),
                        officeName,
                        startScheduleTime,
                        endScheduleTime,
                        bookingHoursQuantity,
                        totalPrice,
                        officeBranchLocation.getProvince(),
                        officeBranchLocation.getCity(),
                        officeBranchLocation.getZipCode(),
                        officeBranchLocation.getStreet()
                )).build()
        );
    }
}

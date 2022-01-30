package booking.domain.booking;

import booking.application.dto.booking.BookingResponse;
import booking.application.dto.booking.BookingScheduleTimeResponse;
import booking.domain.office.Office;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import static java.lang.String.format;

@Table(name = "bookings")
@Entity
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class Booking {
    @EmbeddedId
    private BookingId id;
    @Embedded
    private ScheduleTime scheduleTime;
    @Column
    private LocalDateTime created;
    @Column
    private Integer totalAmount;
    @Column
    private LocalDate confirmationDate;
    @Column
    private Integer attendeesQuantity;
    @Column
    private String renterEmail;
    @Enumerated(EnumType.STRING)
    private Status status;
    @ManyToOne(fetch = FetchType.LAZY)
    private Office office;
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private PaymentInformation paymentInformation;

    private static LocalDateTime toUTC(ZonedDateTime dateTime) {
        return dateTime.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
    }

    public static Try<Booking> create(
            BookingId id,
            Office office,
            ZonedDateTime startTime,
            ZonedDateTime endTime,
            String renterEmail,
            Integer attendeesQuantity
    ) {
        if (endTime.equals(startTime) || endTime.isBefore(startTime))
            return Try.failure(new IllegalArgumentException("Invalid schedule time"));
        if (startTime.getMinute() != 0 || endTime.getMinute() != 0)
            return Try.failure(new IllegalArgumentException("Minutes must be 0"));
        var created = LocalDateTime.now(Clock.systemUTC()).withSecond(0).withNano(0);
        var scheduleTime = ScheduleTime.create(toUTC(startTime), toUTC(endTime), startTime.getZone());
        return Try.success(new Booking(
                id,
                scheduleTime,
                created,
                office.price() * (toUTC(endTime).getHour() - toUTC(startTime).getHour()),
                null,
                attendeesQuantity,
                renterEmail,
                Status.PENDING,
                office,
                null
        ));
    }

    public BookingId id() { return id; }

    public Office office() { return office; }

    public String renterEmail() { return renterEmail; }

    public BookingResponse toResponse() {
        var paymentInformation = this.paymentInformation == null ? null : BookingResponse.PaymentInformation.of(
                this.paymentInformation.getId(),
                this.paymentInformation.getExternalId(),
                this.paymentInformation.getTransactionAmount(),
                this.paymentInformation.getProviderFee(),
                this.paymentInformation.getCurrency(),
                this.paymentInformation.getPaymentMethodId(),
                this.paymentInformation.getPaymentTypeId()
        );
        return BookingResponse.of(
                id.toString(),
                isActive() ? status.name() : Status.CANCELLED.name(),
                attendeesQuantity,
                totalAmount,
                created,
                startScheduleTime(),
                endScheduleTime(),
                paymentInformation,
                office.id().toString(),
                office.name(),
                office.officeBranchId()
        );
    }

    public BookingResponse toResponse(Office office) {
        var paymentInformation = this.paymentInformation == null ? null : BookingResponse.PaymentInformation.of(
                this.paymentInformation.getId(),
                this.paymentInformation.getExternalId(),
                this.paymentInformation.getTransactionAmount(),
                this.paymentInformation.getProviderFee(),
                this.paymentInformation.getCurrency(),
                this.paymentInformation.getPaymentMethodId(),
                this.paymentInformation.getPaymentTypeId()
        );
        return BookingResponse.of(
                id.toString(),
                isActive() ? status.name() : Status.CANCELLED.name(),
                attendeesQuantity,
                totalAmount,
                created,
                startScheduleTime(),
                endScheduleTime(),
                paymentInformation,
                office.id().toString(),
                office.name(),
                office.officeBranchId()
        );
    }

    public BookingScheduleTimeResponse toScheduleTimeResponse() {
        return BookingScheduleTimeResponse.of(
                startScheduleTime().toLocalDate(),
                startScheduleTime().toLocalTime(),
                endScheduleTime().toLocalTime()
        );
    }

    /**
     * @param startTime Start time in UTC
     * @param endTime   End time in UTC
     */
    public boolean hasConflictsWithProposedTime(
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        return scheduleTime.hasConflicts(startTime, endTime);
    }

    public void markAsScheduled(PaymentInformation paymentInformation) {
        this.status = Status.SCHEDULED;
        this.paymentInformation = paymentInformation;
        this.confirmationDate = LocalDate.now(Clock.systemUTC());
    }

    /**
     * A booking is active when it is already scheduled or
     * it is pending for a payment confirmation
     */
    public boolean isActive() {
        return status.equals(Status.SCHEDULED) || isPending();
    }

    public boolean isScheduled() {
        return this.status.equals(Status.SCHEDULED);
    }

    /**
     * Pending status expires after an hour
     */
    public boolean isPending() {
        var now = LocalDateTime.now(Clock.systemUTC());
        return status.equals(Status.PENDING) && now.isBefore(created.plusHours(1));
    }

    public Integer amountOfHours() {
        return scheduleTime.endTime().getHour() - scheduleTime.startTime().getHour();
    }

    public String description() {
        return format(
                "Office renting from %s to %s, total time %s",
                scheduleTime.startTime(),
                scheduleTime.endTime(),
                amountOfHours()
        );
    }

    /**
     * @return start schedule time in booking timezone
     */
    public LocalDateTime startScheduleTime() {
        return scheduleTime.startTime().toLocalDateTime();
    }

    public LocalDateTime startScheduleTimeUTC() {
        return scheduleTime.startTimeUTC();
    }

    /**
     * @return end schedule time in booking timezone
     */
    public LocalDateTime endScheduleTime() {
        return scheduleTime.endTime().toLocalDateTime();
    }

    public LocalDateTime endScheduleTimeUTC() {
        return scheduleTime.endTimeUTC();
    }

    public Integer totalAmount() { return totalAmount; }

    public BookingConfirmedEvent bookingConfirmedEvent() {
        return BookingConfirmedEvent.of(
                id.toString(),
                office.officeBranchId(),
                office.id().toString(),
                paymentInformation.getTransactionAmount(),
                confirmationDate,
                renterEmail
        );
    }
}

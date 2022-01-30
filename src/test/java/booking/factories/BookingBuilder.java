package booking.factories;

import booking.domain.booking.Booking;
import booking.domain.booking.BookingId;
import booking.domain.booking.PaymentInformation;
import booking.domain.booking.Status;
import booking.domain.office.Office;
import com.github.javafaker.Faker;

import java.time.Clock;
import java.time.ZonedDateTime;

public class BookingBuilder {
    Faker faker = Faker.instance();
    private BookingId id = new BookingId();
    private String renterEmail = faker.internet().emailAddress();
    private ZonedDateTime startTime = null;
    private ZonedDateTime endTime = null;
    private Integer attendeesQuantity = faker.number().numberBetween(1, 100);
    private Office office = new OfficeBuilder().build();
    private Status status = null;

    public BookingBuilder withRenterEmail(String email) {
        this.renterEmail = email;
        return this;
    }

    public BookingBuilder withOffice(Office office) {
        this.office = office;
        return this;
    }

    public BookingBuilder withStatus(Status status) {
        this.status = status;
        return this;
    }

    public BookingBuilder withStartTime(ZonedDateTime startTime) {
        this.startTime = startTime;
        if (this.endTime == null)
            this.endTime = this.startTime.plusHours(1);
        return this;
    }

    public BookingBuilder withEndTime(ZonedDateTime endTime) {
        this.endTime = endTime;
        if (this.startTime == null)
            this.startTime = this.endTime.minusHours(1);
        return this;
    }

    public Booking build() {
        if (startTime == null && endTime == null) {
            this.startTime = ZonedDateTime.now(Clock.systemUTC()).withMinute(0);
            this.endTime = startTime.plusHours(1);
        }
        var booking = Booking.create(
                id,
                office,
                startTime,
                endTime,
                renterEmail,
                attendeesQuantity
        ).get();
        if (status != null && status.equals(Status.SCHEDULED))
            booking.markAsScheduled(new PaymentInformation(
                    "12-external",
                    booking.totalAmount().floatValue(),
                    8f,
                    "ARS",
                    "visa",
                    "credit_card"
            ));
        return booking;
    }
}

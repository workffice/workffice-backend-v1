package booking.infrastructure.repositories;

import booking.domain.booking.Booking;
import booking.domain.booking.BookingId;
import booking.domain.booking.PaymentInformation;
import booking.domain.office.Office;
import booking.factories.BookingBuilder;
import booking.factories.OfficeBuilder;
import com.github.javafaker.Faker;
import io.vavr.control.Try;
import server.WorkfficeApplication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = {WorkfficeApplication.class})
public class TestBookingMySQLRepository {

    @Autowired
    BookingMySQLRepository bookingRepo;
    @Autowired
    OfficeMySQLRepository officeRepo;

    Faker faker = Faker.instance();
    ZoneId timezoneARG = ZoneId.of("America/Argentina/Buenos_Aires");

    Office createOffice() {
        var office = new OfficeBuilder().build();
        officeRepo.store(office);
        return office;
    }

    @Test
    void itShouldStoreBookingWithInfoSpecified() {
        var bookingId = new BookingId();
        var startTime = ZonedDateTime.of(
                LocalDateTime.of(2018, 12, 8, 14, 0, 0),
                timezoneARG
        );
        var endTime = ZonedDateTime.of(
                LocalDateTime.of(2018, 12, 8, 16, 0, 0),
                timezoneARG
        );
        var booking = Booking.create(
                bookingId,
                createOffice(),
                startTime,
                endTime,
                "test@email.com",
                10
        ).get();

        Try<Void> response = bookingRepo.store(booking);

        assertThat(response.isSuccess()).isTrue();
        var bookingSaved = bookingRepo.findById(bookingId).get();
        assertThat(bookingSaved.toResponse()).isEqualTo(booking.toResponse());

        assertThat(bookingSaved.toResponse().getStartTime()).isEqualTo(
                LocalDateTime.of(2018, 12, 8, 14, 0)
        );
        assertThat(bookingSaved.toResponse().getEndTime()).isEqualTo(
                LocalDateTime.of(2018, 12, 8, 16, 0)
        );
    }

    @Test
    void itShouldReturnAllBookingsRelatedWithSpecifiedOfficeAtProposedDate() {
        var startTimeExample = ZonedDateTime.of(
                LocalDateTime.of(2018, 12, 8, 14, 0, 0),
                timezoneARG
        );

        var office1 = createOffice();
        var office2 = createOffice();

        var booking1 = new BookingBuilder()
                .withStartTime(startTimeExample)
                .withOffice(office1)
                .build();
        var booking2 = new BookingBuilder()
                .withStartTime(startTimeExample.plusHours(1))
                .withOffice(office1)
                .build();
        var booking3 = new BookingBuilder()
                .withStartTime(startTimeExample.plusDays(1))
                .withOffice(office1)
                .build();
        var booking4 = new BookingBuilder()
                .withStartTime(startTimeExample)
                .withOffice(office2)
                .build();
        var booking5 = new BookingBuilder()
                .withStartTime(startTimeExample.plusHours(1))
                .withOffice(office2)
                .build();
        bookingRepo.store(booking1);
        bookingRepo.store(booking2);
        bookingRepo.store(booking3);
        bookingRepo.store(booking4);
        bookingRepo.store(booking5);

        var bookings = bookingRepo.find(office1, startTimeExample.toLocalDate());

        assertThat(bookings).size().isEqualTo(2);
        assertThat(bookings).map(Booking::id).containsExactlyInAnyOrder(
                booking1.id(),
                booking2.id()
        );
    }

    @Test
    void itShouldUpdateBooking() {
        var office1 = createOffice();
        var startTimeExample = ZonedDateTime.of(
                LocalDateTime.of(2018, 12, 8, 14, 0, 0),
                timezoneARG
        );
        var booking = new BookingBuilder()
                .withStartTime(startTimeExample)
                .withOffice(office1)
                .build();
        bookingRepo.store(booking);

        booking.markAsScheduled(new PaymentInformation(
                1L,
                "12-external",
                100f,
                8f,
                "ARS",
                "visa",
                "credit_card"
        ));
        bookingRepo.update(booking);

        var bookingUpdated = bookingRepo.findById(booking.id()).get();
        assertThat(bookingUpdated.toResponse()).isEqualTo(booking.toResponse());
    }

    @Test
    void itShouldReturnAllCurrentBookingsRelatedWithRenterEmail() {
        var office1 = createOffice();
        var email = faker.internet().emailAddress();
        var booking1 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 8,
                        16, 0, 0, 0,
                        timezoneARG))
                .withOffice(office1)
                .withRenterEmail(email)
                .build();
        var booking2 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 8,
                        9, 0, 0, 0,
                        timezoneARG))
                .withOffice(office1)
                .withRenterEmail(email)
                .build();
        var booking3 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 7,
                        13, 0, 0, 0,
                        timezoneARG))
                .withOffice(office1)
                .withRenterEmail(email)
                .build();
        var booking4 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 6,
                        13, 0, 0, 0,
                        timezoneARG))
                .withOffice(office1)
                .withRenterEmail(email)
                .build();
        var booking5 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 8,
                        13, 0, 0, 0,
                        timezoneARG))
                .withOffice(office1)
                .withRenterEmail(email)
                .build();
        bookingRepo.store(booking1);
        bookingRepo.store(booking2);
        bookingRepo.store(booking3);
        bookingRepo.store(booking4);
        bookingRepo.store(booking5);

        var today = LocalDate.of(2021, 12, 8);
        var bookings = bookingRepo.find(email, true, today, 0, 2);
        var bookings2 = bookingRepo.find(email, true, today, 2, 4);

        assertThat(bookings).size().isEqualTo(2);
        assertThat(bookings).map(Booking::toResponse).containsExactly(booking2.toResponse(), booking5.toResponse());
        assertThat(bookings2).size().isEqualTo(1);
        assertThat(bookings2).map(Booking::toResponse).containsExactly(booking1.toResponse());
    }

    @Test
    void itShouldReturnAllCurrentBookingsQuantityRelatedWithRenterEmail() {
        var office1 = createOffice();
        var email = faker.internet().emailAddress();
        var booking1 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 8,
                        16, 0, 0, 0,
                        timezoneARG))
                .withOffice(office1)
                .withRenterEmail(email)
                .build();
        var booking2 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 8,
                        9, 0, 0, 0,
                        timezoneARG))
                .withOffice(office1)
                .withRenterEmail(email)
                .build();
        var booking3 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 7,
                        13, 0, 0, 0,
                        timezoneARG))
                .withOffice(office1)
                .withRenterEmail(email)
                .build();
        var booking4 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 6,
                        13, 0, 0, 0,
                        timezoneARG))
                .withOffice(office1)
                .withRenterEmail(email)
                .build();
        var booking5 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 8,
                        13, 0, 0, 0,
                        timezoneARG))
                .withOffice(office1)
                .withRenterEmail(email)
                .build();
        bookingRepo.store(booking1);
        bookingRepo.store(booking2);
        bookingRepo.store(booking3);
        bookingRepo.store(booking4);
        bookingRepo.store(booking5);

        var today = LocalDate.of(2021, 12, 8);
        var bookingsQuantity = bookingRepo.count(email, true, today);

        assertThat(bookingsQuantity).isEqualTo(3);
    }

    @Test
    void itShouldReturnAllPastBookingsRelatedWithRenterEmail() {
        var office1 = createOffice();
        var email = faker.internet().emailAddress();
        var booking1 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 4,
                        9, 0, 0, 0,
                        timezoneARG))
                .withOffice(office1)
                .withRenterEmail(email)
                .build();
        var booking2 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 4,
                        16, 0, 0, 0,
                        timezoneARG))
                .withOffice(office1)
                .withRenterEmail(email)
                .build();
        var booking3 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 7,
                        13, 0, 0, 0,
                        timezoneARG))
                .withOffice(office1)
                .withRenterEmail(email)
                .build();
        var booking4 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 6,
                        13, 0, 0, 0,
                        timezoneARG))
                .withOffice(office1)
                .withRenterEmail(email)
                .build();
        var booking5 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 8,
                        13, 0, 0, 0,
                        timezoneARG))
                .withOffice(office1)
                .withRenterEmail(email)
                .build();
        bookingRepo.store(booking1);
        bookingRepo.store(booking2);
        bookingRepo.store(booking3);
        bookingRepo.store(booking4);
        bookingRepo.store(booking5);

        var today = LocalDate.of(2021, 12, 8);
        var bookings = bookingRepo.find(email, false, today, 0, 2);
        var bookings2 = bookingRepo.find(email, false, today, 2, 4);

        assertThat(bookings).size().isEqualTo(2);
        assertThat(bookings).map(Booking::toResponse).containsExactly(booking3.toResponse(), booking4.toResponse());
        assertThat(bookings2).size().isEqualTo(2);
        assertThat(bookings2).map(Booking::toResponse).containsExactly(booking2.toResponse(), booking1.toResponse());
    }

    @Test
    void itShouldReturnPastBookingsQuantityRelatedWithRenterEmail() {
        var office1 = createOffice();
        var email = faker.internet().emailAddress();
        var booking1 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 4,
                        9, 0, 0, 0,
                        timezoneARG))
                .withOffice(office1)
                .withRenterEmail(email)
                .build();
        var booking2 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 4,
                        16, 0, 0, 0,
                        timezoneARG))
                .withOffice(office1)
                .withRenterEmail(email)
                .build();
        var booking3 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 7,
                        13, 0, 0, 0,
                        timezoneARG))
                .withOffice(office1)
                .withRenterEmail(email)
                .build();
        var booking4 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 6,
                        13, 0, 0, 0,
                        timezoneARG))
                .withOffice(office1)
                .withRenterEmail(email)
                .build();
        var booking5 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 8,
                        13, 0, 0, 0,
                        timezoneARG))
                .withOffice(office1)
                .withRenterEmail(email)
                .build();
        bookingRepo.store(booking1);
        bookingRepo.store(booking2);
        bookingRepo.store(booking3);
        bookingRepo.store(booking4);
        bookingRepo.store(booking5);

        var today = LocalDate.of(2021, 12, 8);
        var bookingsQuantity = bookingRepo.count(email, false, today);

        assertThat(bookingsQuantity).isEqualTo(4);
    }

    @Test
    void itShouldReturnTrueWhenRenterEmailHasBookedOffice() {
        var office = createOffice();
        var email = faker.internet().emailAddress();
        var booking1 = new BookingBuilder()
                .withOffice(office)
                .withRenterEmail(email)
                .build();
        var booking2 = new BookingBuilder()
                .withOffice(office)
                .withRenterEmail("another@email.com")
                .build();
        var booking3 = new BookingBuilder()
                .withOffice(office)
                .withRenterEmail(email)
                .build();
        bookingRepo.store(booking1);
        bookingRepo.store(booking2);
        bookingRepo.store(booking3);

        assertThat(bookingRepo.exists(email, office)).isTrue();
    }

    @Test
    void itShouldReturnFalseWhenRenterEmailHasNotBookedOffice() {
        var office = createOffice();
        var email = faker.internet().emailAddress();
        var booking1 = new BookingBuilder()
                .withOffice(office)
                .withRenterEmail(email)
                .build();
        bookingRepo.store(booking1);

        assertThat(bookingRepo.exists("unexistent@email.com", office)).isFalse();
    }
}

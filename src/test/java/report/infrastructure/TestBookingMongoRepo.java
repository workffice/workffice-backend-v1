package report.infrastructure;

import report.domain.Booking;
import report.domain.OfficeBookedProjection;
import report.domain.OfficeTransactionAmountProjection;
import report.domain.TransactionAmountProjection;
import server.WorkfficeApplication;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = WorkfficeApplication.class)
public class TestBookingMongoRepo {
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    BookingMongoRepo bookingRepo;

    @BeforeEach
    void tearDown() {
        var collection = mongoTemplate.getCollection("bookings");
        collection.drop();
    }

    @Test
    void itShouldStoreBookingWithInformationSpecified() {
        var booking = Booking.create(
                "1",
                "1",
                "23",
                1000f,
                LocalDate.now()
        );

        bookingRepo.store(booking);

        var bookingStored = bookingRepo.findById("1").get();
        assertThat(bookingStored).isEqualTo(booking);
    }

    @Test
    void itShouldReturnOfficesOrderedByTotalTransactionAmount() {
        var booking1 = Booking.create(
                "1",
                "1",
                "23",
                1000f,
                LocalDate.of(2021, 9, 28)
        );
        var booking2 = Booking.create(
                "2",
                "1",
                "23",
                1000f,
                LocalDate.of(2021, 9, 11)
        );
        var booking3 = Booking.create(
                "3",
                "1",
                "27",
                500f,
                LocalDate.of(2021, 9, 2)
        );
        var booking4 = Booking.create(
                "4",
                "1",
                "27",
                10f,
                LocalDate.of(2021, 9, 10)
        );
        // Booking 5 was confirmed in October
        var booking5 = Booking.create(
                "5",
                "1",
                "27",
                5000f,
                LocalDate.of(2021, 10, 10)
        );
        var booking6 = Booking.create(
                "6",
                "1",
                "27",
                10f,
                LocalDate.of(2020, 9, 10)
        );
        bookingRepo.store(booking1);
        bookingRepo.store(booking2);
        bookingRepo.store(booking3);
        bookingRepo.store(booking4);
        bookingRepo.store(booking5);
        bookingRepo.store(booking6);

        List<OfficeTransactionAmountProjection> offices = bookingRepo
                .officesTransactionAmountReport("1", Month.SEPTEMBER);

        assertThat(offices).size().isEqualTo(2);
        assertThat(offices).containsExactly(
                OfficeTransactionAmountProjection.of("23", "SEPTEMBER", 2000f),
                OfficeTransactionAmountProjection.of("27", "SEPTEMBER", 510f)
        );
    }

    @Test
    void itShouldReturnOfficesOrderedByTotalBookings() {
        var booking1 = Booking.create(
                "1",
                "1",
                "23",
                1000f,
                LocalDate.of(2021, 9, 28)
        );
        var booking2 = Booking.create(
                "2",
                "1",
                "22",
                1000f,
                LocalDate.of(2021, 9, 11)
        );
        var booking3 = Booking.create(
                "3",
                "1",
                "23",
                500f,
                LocalDate.of(2021, 9, 2)
        );
        var booking4 = Booking.create(
                "4",
                "1",
                "23",
                10f,
                LocalDate.of(2021, 9, 10)
        );
        // Booking 5 was confirmed in October
        var booking5 = Booking.create(
                "5",
                "1",
                "27",
                5000f,
                LocalDate.of(2021, 10, 10)
        );
        var booking6 = Booking.create(
                "6",
                "1",
                "21",
                5000f,
                LocalDate.of(2021, 9, 15)
        );
        var booking7 = Booking.create(
                "7",
                "1",
                "21",
                5000f,
                LocalDate.of(2020, 9, 15)
        );
        bookingRepo.store(booking1);
        bookingRepo.store(booking2);
        bookingRepo.store(booking3);
        bookingRepo.store(booking4);
        bookingRepo.store(booking5);
        bookingRepo.store(booking6);
        bookingRepo.store(booking7);

        List<OfficeBookedProjection> offices = bookingRepo
                .officeBookedReport("1", Month.SEPTEMBER);

        assertThat(offices).size().isEqualTo(3);
        assertThat(offices).containsExactly(
                OfficeBookedProjection.of("23", "SEPTEMBER", 3),
                OfficeBookedProjection.of("22", "SEPTEMBER", 1),
                OfficeBookedProjection.of("21", "SEPTEMBER", 1)
        );
    }

    @Test
    void itShouldReturnTotalAmountForSpecificYearOrderedByMonth() {
        var booking1 = Booking.create(
                "1",
                "1",
                "23",
                1000f,
                LocalDate.of(2021, 1, 28)
        );
        var booking2 = Booking.create(
                "2",
                "1",
                "22",
                1000f,
                LocalDate.of(2021, 1, 11)
        );
        var booking3 = Booking.create(
                "3",
                "1",
                "23",
                500f,
                LocalDate.of(2021, 3, 2)
        );
        var booking4 = Booking.create(
                "4",
                "1",
                "23",
                10f,
                LocalDate.of(2021, 4, 10)
        );
        var booking5 = Booking.create(
                "5",
                "1",
                "27",
                5000f,
                LocalDate.of(2021, 10, 10)
        );
        var booking6 = Booking.create(
                "6",
                "1",
                "21",
                5000f,
                LocalDate.of(2021, 10, 15)
        );
        var booking7 = Booking.create(
                "7",
                "1",
                "21",
                5000f,
                LocalDate.of(2020, 10, 15)
        );
        bookingRepo.store(booking1);
        bookingRepo.store(booking2);
        bookingRepo.store(booking3);
        bookingRepo.store(booking4);
        bookingRepo.store(booking5);
        bookingRepo.store(booking6);
        bookingRepo.store(booking7);

        var transactionAmounts = bookingRepo
                .transactionAmountReport("1", Year.of(2021));

        assertThat(transactionAmounts).size().isEqualTo(4);
        assertThat(transactionAmounts).containsExactly(
                TransactionAmountProjection.of(2021, "OCTOBER", 10000f),
                TransactionAmountProjection.of(2021, "APRIL", 10f),
                TransactionAmountProjection.of(2021, "MARCH", 500f),
                TransactionAmountProjection.of(2021, "JANUARY", 2000f)
        );
    }
}

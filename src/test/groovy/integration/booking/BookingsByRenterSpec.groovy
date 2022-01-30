package integration.booking

import booking.domain.booking.BookingRepository
import booking.domain.booking.Status
import booking.domain.office.OfficeRepository
import booking.factories.BookingBuilder
import booking.factories.OfficeBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.javafaker.Faker
import controller.AuthUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import server.WorkfficeApplication
import spock.lang.Specification

import java.time.*

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class BookingsByRenterSpec extends Specification {
    @Autowired
    OfficeRepository officeRepo
    @Autowired
    BookingRepository bookingRepo
    @Autowired
    AuthUtil authUtil
    @Autowired
    ObjectMapper objectMapper
    @Autowired
    MockMvc mockMvc

    Faker faker = Faker.instance()
    ZoneId timezoneARG = ZoneId.of("America/Argentina/Buenos_Aires")

    void "it should return not authorized when token is not provided"() {
        when:
        def response = mockMvc.perform(get("/api/bookings/?renter_email=napoleon@mail.com&current_bookings=true"))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return forbidden when auth user tries to access to another user bookings"() {
        given:
        def office = new OfficeBuilder().build()
        def email = faker.internet().emailAddress()
        def booking1 = new BookingBuilder()
                .withRenterEmail(email)
                .withOffice(office)
                .build()
        officeRepo.store(office)
        bookingRepo.store(booking1)
        and:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(get("/api/bookings/?renter_email=${email}&current_bookings=true")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'BOOKING_FORBIDDEN',
                            'message': 'You don\'t have access to this booking',
                    ]
            ]
        }
    }

    void "it should return ok with current bookings"() {
        given:
        def office = new OfficeBuilder()
                .withPrice(1300)
                .build()
        def email = faker.internet().emailAddress()
        def booking1 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        LocalDate.now(Clock.systemUTC()).plusDays(4),
                        LocalTime.of(10, 0),
                        timezoneARG))
                .withStatus(Status.SCHEDULED)
                .withRenterEmail(email)
                .withOffice(office)
                .build()
        def booking2 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        LocalDate.now(Clock.systemUTC()).minusDays(4),
                        LocalTime.of(10, 0),
                        timezoneARG))
                .withRenterEmail(email)
                .withOffice(office)
                .build()
        def booking3 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        LocalDate.now(Clock.systemUTC()),
                        LocalTime.of(14, 0).plusHours(1),
                        timezoneARG))
                .withRenterEmail(email)
                .withOffice(office)
                .build()
        def booking4 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        LocalDate.now(Clock.systemUTC()).minusDays(1),
                        LocalTime.of(10, 0),
                        timezoneARG))
                .withRenterEmail(email)
                .withOffice(office)
                .build()
        officeRepo.store(office)
        bookingRepo.store(booking1)
        bookingRepo.store(booking2)
        bookingRepo.store(booking3)
        bookingRepo.store(booking4)
        and:
        def token = authUtil.createAndLoginUser(email, "1234")

        when:
        def response = mockMvc.perform(get("/api/bookings/?renter_email=${email}&current_bookings=true")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and: "Renter's current bookings ordered by the more nearest to happen"
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data.size() == 2
            it.data == [
                    [
                            'id'               : booking3.toResponse().id,
                            'status'           : 'PENDING',
                            'attendeesQuantity': booking3.toResponse().attendeesQuantity,
                            'totalAmount'      : 1300f,
                            'created'          : booking3.toResponse().created.toString() + ":00",
                            'startTime'        : booking3.toResponse().startTime.toString() + ":00",
                            'endTime'          : booking3.toResponse().endTime.toString() + ":00",
                            'payment'          : null,
                            'officeId'         : booking3.toResponse().officeId,
                            'officeName'       : booking3.toResponse().officeName,
                            'officeBranchId'   : booking3.toResponse().officeBranchId,
                    ],
                    [
                            'id'               : booking1.toResponse().id,
                            'status'           : 'SCHEDULED',
                            'attendeesQuantity': booking1.toResponse().attendeesQuantity,
                            'totalAmount'      : 1300f,
                            'created'          : booking1.toResponse().created.toString() + ":00",
                            'startTime'        : booking1.toResponse().startTime.toString() + ":00",
                            'endTime'          : booking1.toResponse().endTime.toString() + ":00",
                            'payment'          : [
                                    'id'               : booking1.toResponse().payment.id,
                                    'externalId'       : '12-external',
                                    'transactionAmount': 1300,
                                    'providerFee'      : 8f,
                                    'currency'         : 'ARS',
                                    'paymentMethodId'  : 'visa',
                                    'paymentTypeId'    : 'credit_card',
                            ],
                            'officeId'         : booking1.toResponse().officeId,
                            'officeName'       : booking1.toResponse().officeName,
                            'officeBranchId'   : booking1.toResponse().officeBranchId,
                    ]
            ]
        }
    }

    void "it should return ok with past bookings"() {
        given:
        def office = new OfficeBuilder()
                .withPrice(1300)
                .build()
        def email = faker.internet().emailAddress()
        def booking1 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        LocalDate.now(Clock.systemUTC()).plusDays(4),
                        LocalTime.of(10, 0),
                        timezoneARG))
                .withStatus(Status.SCHEDULED)
                .withRenterEmail(email)
                .withOffice(office)
                .build()
        def booking2 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        LocalDate.now(Clock.systemUTC()).minusDays(4),
                        LocalTime.of(10, 0),
                        timezoneARG))
                .withRenterEmail(email)
                .withOffice(office)
                .build()
        def booking3 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        LocalDate.now(Clock.systemUTC()),
                        LocalTime.of(14, 0).plusHours(1),
                        timezoneARG))
                .withRenterEmail(email)
                .withOffice(office)
                .build()
        def booking4 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        LocalDate.now(Clock.systemUTC()).minusDays(1),
                        LocalTime.of(10, 0),
                        timezoneARG))
                .withStatus(Status.SCHEDULED)
                .withRenterEmail(email)
                .withOffice(office)
                .build()
        officeRepo.store(office)
        bookingRepo.store(booking1)
        bookingRepo.store(booking2)
        bookingRepo.store(booking3)
        bookingRepo.store(booking4)
        and:
        def token = authUtil.createAndLoginUser(email, "1234")

        when:
        def response = mockMvc.perform(get("/api/bookings/?renter_email=${email}&current_bookings=false")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and: "Renter's past bookings since yesterdays ordered by the nearest"
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data.size() == 2
            it.data == [
                    [
                            'id'               : booking4.toResponse().id,
                            'status'           : 'SCHEDULED',
                            'attendeesQuantity': booking4.toResponse().attendeesQuantity,
                            'totalAmount'      : 1300f,
                            'created'          : booking4.toResponse().created.toString() + ":00",
                            'startTime'        : booking4.toResponse().startTime.toString() + ":00",
                            'endTime'          : booking4.toResponse().endTime.toString() + ":00",
                            'payment'          : [
                                    'id'               : booking4.toResponse().payment.id,
                                    'externalId'       : '12-external',
                                    'transactionAmount': 1300f,
                                    'providerFee'      : 8f,
                                    'currency'         : 'ARS',
                                    'paymentMethodId'  : 'visa',
                                    'paymentTypeId'    : 'credit_card',
                            ],
                            'officeId'         : booking4.toResponse().officeId,
                            'officeName'       : booking4.toResponse().officeName,
                            'officeBranchId'   : booking4.toResponse().officeBranchId,
                    ],
                    [
                            'id'               : booking2.toResponse().id,
                            'status'           : 'PENDING', // Should be CANCELLED but since the booking is created right now it still pending
                            'attendeesQuantity': booking2.toResponse().attendeesQuantity,
                            'totalAmount'      : 1300f,
                            'created'          : booking2.toResponse().created.toString() + ":00",
                            'startTime'        : booking2.toResponse().startTime.toString() + ":00",
                            'endTime'          : booking2.toResponse().endTime.toString() + ":00",
                            'payment'          : null,
                            'officeId'         : booking2.toResponse().officeId,
                            'officeName'       : booking2.toResponse().officeName,
                            'officeBranchId'   : booking2.toResponse().officeBranchId,
                    ],
            ]
        }
    }

    void "it should return ok with pagination information"() {
        given:
        def office = new OfficeBuilder()
                .withPrice(1300)
                .build()
        def email = faker.internet().emailAddress()
        def booking1 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        LocalDate.now(Clock.systemUTC()).plusDays(4),
                        LocalTime.of(10, 0),
                        timezoneARG))
                .withStatus(Status.SCHEDULED)
                .withRenterEmail(email)
                .withOffice(office)
                .build()
        def booking2 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        LocalDate.now(Clock.systemUTC()).plusDays(4),
                        LocalTime.of(10, 0),
                        timezoneARG))
                .withRenterEmail(email)
                .withOffice(office)
                .build()
        def booking3 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        LocalDate.now(Clock.systemUTC()).plusDays(1),
                        LocalTime.of(14, 0),
                        timezoneARG))
                .withRenterEmail(email)
                .withOffice(office)
                .build()
        def booking4 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        LocalDate.now(Clock.systemUTC()).plusDays(1),
                        LocalTime.of(10, 0),
                        timezoneARG))
                .withRenterEmail(email)
                .withOffice(office)
                .build()
        officeRepo.store(office)
        bookingRepo.store(booking1)
        bookingRepo.store(booking2)
        bookingRepo.store(booking3)
        bookingRepo.store(booking4)
        and:
        def token = authUtil.createAndLoginUser(email, "1234")

        when:
        def response = mockMvc.perform(get("/api/bookings/?renter_email=${email}&current_bookings=true&size=2")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and: "Renter's current bookings ordered by the more nearest to happen"
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data.size() == 2
            it.pagination == [
                    pageSize   : 2,
                    lastPage   : false,
                    totalPages : 2,
                    currentPage: 1,
            ]
        }
    }
}

package integration.booking

import booking.domain.booking.BookingId
import booking.domain.booking.BookingRepository
import booking.domain.booking.Status
import booking.domain.office.OfficeRepository
import booking.factories.BookingBuilder
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class BookingFinderSpec extends Specification {
    @Autowired
    OfficeRepository officeRepo
    @Autowired
    BookingRepository bookingRepo
    @Autowired
    AuthUtil authUtil
    @Autowired
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper

    Faker faker = Faker.instance()

    void "it should return not authorized when token is not provided"() {
        when:
        def response = mockMvc.perform(get("/api/bookings/1/"))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return bad request when booking id is invalid"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(get("/api/bookings/-1/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INVALID_BOOKING_ID',
                            'message': 'Booking id provided is not valid',
                    ]
            ]
        }
    }

    void "it should return not found when booking does not exist"() {
        given:
        def bookingId = new BookingId()
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(get("/api/bookings/${bookingId}/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'NOT_FOUND',
                            'error'  : 'BOOKING_NOT_FOUND',
                            'message': 'There is no booking with specified id',
                    ]
            ]
        }
    }

    void "it should return forbidden when auth user is not the renter of the booking"() {
        given:
        def booking = new BookingBuilder().build()
        officeRepo.store(booking.office())
        bookingRepo.store(booking)
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(get("/api/bookings/${booking.id()}/")
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

    void "it should return ok with booking scheduled information"() {
        given:
        def booking = new BookingBuilder()
                .withStatus(Status.SCHEDULED)
                .build()
        officeRepo.store(booking.office())
        bookingRepo.store(booking).get()
        def token = authUtil.createAndLoginUser(booking.renterEmail(), "1234")

        when:
        def response = mockMvc.perform(get("/api/bookings/${booking.id()}/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == [
                    'id'               : booking.toResponse().id,
                    'status'           : 'SCHEDULED',
                    'attendeesQuantity': booking.toResponse().attendeesQuantity,
                    'totalAmount'      : booking.totalAmount().floatValue(),
                    'created'          : booking.toResponse().created.toString() + ":00",
                    'startTime'        : booking.toResponse().startTime.toString() + ":00",
                    'endTime'          : booking.toResponse().endTime.toString() + ":00",
                    'payment'          : [
                            'id'               : booking.toResponse().payment.id,
                            'externalId'       : '12-external',
                            'transactionAmount': booking.totalAmount().floatValue(),
                            'providerFee'      : 8f,
                            'currency'         : 'ARS',
                            'paymentMethodId'  : 'visa',
                            'paymentTypeId'    : 'credit_card',
                    ],
                    'officeId'         : booking.toResponse().officeId,
                    'officeName'       : booking.toResponse().officeName,
                    'officeBranchId'   : booking.toResponse().officeBranchId,
            ]
        }
    }

    void "it should return ok with booking pending information"() {
        given:
        def booking = new BookingBuilder()
                .withStatus(Status.PENDING)
                .build()
        officeRepo.store(booking.office())
        bookingRepo.store(booking)
        def token = authUtil.createAndLoginUser(booking.renterEmail(), "1234")

        when:
        def response = mockMvc.perform(get("/api/bookings/${booking.id()}/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == [
                    'id'               : booking.toResponse().id,
                    'status'           : 'PENDING',
                    'attendeesQuantity': booking.toResponse().attendeesQuantity,
                    'totalAmount'      : booking.totalAmount().floatValue(),
                    'created'          : booking.toResponse().created.toString() + ":00",
                    'startTime'        : booking.toResponse().startTime.toString() + ":00",
                    'endTime'          : booking.toResponse().endTime.toString() + ":00",
                    'payment'          : null,
                    'officeId'         : booking.toResponse().officeId,
                    'officeName'       : booking.toResponse().officeName,
                    'officeBranchId'   : booking.toResponse().officeBranchId,
            ]
        }
    }
}

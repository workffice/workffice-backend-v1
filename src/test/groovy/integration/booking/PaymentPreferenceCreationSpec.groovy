package integration.booking

import booking.application.dto.booking.BookingError
import booking.application.dto.booking.BookingPreferenceInformation
import booking.domain.booking.BookingId
import booking.domain.booking.BookingRepository
import booking.domain.payment_preference.PaymentPreference
import booking.domain.payment_preference.PreferenceCreator
import booking.domain.booking.Status
import booking.domain.office.OfficeRepository
import booking.factories.BookingBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.javafaker.Faker
import controller.AuthUtil
import io.vavr.control.Either
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import server.WorkfficeApplication
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class PaymentPreferenceCreationSpec extends Specification {

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
    @MockBean
    PreferenceCreator preferenceCreator

    Faker faker = Faker.instance()

    void "it should return not authorized when token is not provided"() {
        when:
        def response = mockMvc.perform(post("/api/bookings/1/mp_preferences/"))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return bad request when booking id is not uuid format"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(post("/api/bookings/-1/mp_preferences/")
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

    void "it should return not found when booking id does not exist"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def bookingId = new BookingId()

        when:
        def response = mockMvc.perform(post("/api/bookings/${bookingId}/mp_preferences/")
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

    void "it should return forbidden when auth user has not the same email as the renter"() {
        given: "A booking for a renter email 'some@email.com'"
        def booking = new BookingBuilder()
                .withRenterEmail("some@email.com")
                .build()
        officeRepo.store(booking.office())
        bookingRepo.store(booking)
        and: 'An authenticated user with a different email'
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(post("/api/bookings/${booking.id()}/mp_preferences/")
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

    void "it should return bad request when booking is already scheduled"() {
        given:
        def booking = new BookingBuilder()
                .withStatus(Status.SCHEDULED)
                .build()
        officeRepo.store(booking.office())
        bookingRepo.store(booking).get()
        and:
        def token = authUtil.createAndLoginUser(booking.renterEmail(), "1234")

        when:
        def response = mockMvc.perform(post("/api/bookings/${booking.id()}/mp_preferences/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'BOOKING_IS_NOT_PENDING',
                            'message': 'You can\'t create a payment preference for a booking that is no longer pending',
                    ]
            ]
        }
    }

    void "it should return bad request when it fails to create mercado pago preference"() {
        given:
        def booking = new BookingBuilder().build()
        officeRepo.store(booking.office())
        bookingRepo.store(booking)
        // Mock mercado pago response
        Mockito.when(preferenceCreator.createForBooking(Mockito.any(BookingPreferenceInformation.class)))
                .thenReturn(Either.left(BookingError.MERCADO_PAGO_ERROR))
        and:
        def token = authUtil.createAndLoginUser(booking.renterEmail(), "1234")

        when:
        def response = mockMvc.perform(post("/api/bookings/${booking.id()}/mp_preferences/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'MERCADO_PAGO_ERROR',
                            'message': 'Something went wrong with mercado pago try again later',
                    ]
            ]
        }
    }

    void "it should return created with preference id created by mercado pago"() {
        given:
        def booking = new BookingBuilder().build()
        officeRepo.store(booking.office())
        bookingRepo.store(booking)
        // Mock mercado pago response
        Mockito.when(preferenceCreator.createForBooking(Mockito.any(BookingPreferenceInformation.class)))
                .thenReturn(Either.right(PaymentPreference.of("1234")))
        and:
        def token = authUtil.createAndLoginUser(booking.renterEmail(), "1234")

        when:
        def response = mockMvc.perform(post("/api/bookings/${booking.id()}/mp_preferences/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.CREATED.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == ['id': '1234']
        }
    }
}

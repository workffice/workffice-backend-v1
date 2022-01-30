package integration.review

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
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import review.application.dto.ReviewInfo
import review.domain.review.ReviewRepository
import review.factories.ReviewBuilder
import server.WorkfficeApplication
import spock.lang.Specification

import static controller.IntegrationTestUtil.asJsonString
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class ReviewCreationSpec extends Specification {
    @Autowired
    ReviewRepository reviewRepo
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
        given:
        def info = ReviewInfo.of(UUID.randomUUID().toString(), 5, "Some comment", "napoleon@email.com")

        when:
        def response = mockMvc.perform(post("/api/office_branches/1/reviews/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info)))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return forbidden when auth user tries to create a review with a different email"() {
        given:
        def info = ReviewInfo.of(UUID.randomUUID().toString(), 5, "Some comment", "napoleon@email.com")
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(post("/api/office_branches/1/reviews/")
                .header("Authorization", "Bearer ${token.getToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info)))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'REVIEW_FORBIDDEN',
                            'message': 'You can only create a review for your email user',
                    ]
            ]
        }
    }

    void "it should return bad request when the user have already created a review for the same office"() {
        given:
        def renterEmail = faker.internet().emailAddress()
        def officeId = UUID.randomUUID().toString()
        def review = new ReviewBuilder()
                .withOfficeId(officeId)
                .withRenterEmail(renterEmail)
                .build()
        reviewRepo.store(review)
        and:
        def token = authUtil.createAndLoginUser(renterEmail, "1234")
        def info = ReviewInfo.of(officeId, 5, "Some comment", renterEmail)

        when:
        def response = mockMvc.perform(post("/api/office_branches/1/reviews/")
                .header("Authorization", "Bearer ${token.getToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info)))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'REVIEW_ALREADY_CREATED',
                            'message': 'You already review this office',
                    ]
            ]
        }
    }

    void "it should return bad request when user has never booked for the specified office"() {
        given:
        def renterEmail = faker.internet().emailAddress()
        def officeId = UUID.randomUUID().toString()
        and:
        def token = authUtil.createAndLoginUser(renterEmail, "1234")
        def info = ReviewInfo.of(officeId, 5, "Some comment", renterEmail)

        when:
        def response = mockMvc.perform(post("/api/office_branches/1/reviews/")
                .header("Authorization", "Bearer ${token.getToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info)))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'NO_BOOKING',
                            'message': 'You don\'t have bookings to review this office',
                    ]
            ]
        }
    }

    void "it should return bad request when review information is invalid"() {
        given:
        def renterEmail = faker.internet().emailAddress()
        def office = new OfficeBuilder().build()
        def officeId = office.id().toString()
        def booking = new BookingBuilder()
                .withStatus(Status.SCHEDULED)
                .withRenterEmail(renterEmail)
                .withOffice(office)
                .build()
        officeRepo.store(office)
        bookingRepo.store(booking)
        and:
        def token = authUtil.createAndLoginUser(renterEmail, "1234")
        def info = ReviewInfo.of(officeId, 50, "Some comment", renterEmail)

        when:
        def response = mockMvc.perform(post("/api/office_branches/1/reviews/")
                .header("Authorization", "Bearer ${token.getToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info)))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INVALID_REVIEW_INFO',
                            'message': 'Your review information is wrong, stars must be between 1 and 5',
                    ]
            ]
        }
    }

    void "it should return created when review is created successfully"() {
        given:
        def renterEmail = faker.internet().emailAddress()
        def office = new OfficeBuilder().build()
        def officeId = office.id().toString()
        def booking = new BookingBuilder()
                .withStatus(Status.SCHEDULED)
                .withRenterEmail(renterEmail)
                .withOffice(office)
                .build()
        officeRepo.store(office)
        bookingRepo.store(booking)
        and:
        def token = authUtil.createAndLoginUser(renterEmail, "1234")
        def info = ReviewInfo.of(officeId, 3, "Some comment", renterEmail)

        when:
        def response = mockMvc.perform(post("/api/office_branches/1/reviews/")
                .header("Authorization", "Bearer ${token.getToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info)))
                .andReturn().response

        then:
        response.status == HttpStatus.CREATED.value()
    }
}

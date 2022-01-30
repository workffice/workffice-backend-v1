package integration.booking

import booking.application.dto.booking.BookingInformation
import booking.domain.booking.BookingRepository
import booking.domain.office.OfficeId
import booking.domain.office.OfficeRepository
import booking.domain.office.privacy.PrivateOffice
import booking.domain.office.privacy.SharedOffice
import booking.factories.BookingBuilder
import booking.factories.OfficeBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.javafaker.Faker
import controller.AuthUtil
import controller.IntegrationTestUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import server.WorkfficeApplication
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class BookingCreationSpec extends Specification {
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

    private static MockHttpServletRequestBuilder createBookingRequest(String id, Object info, String token) {
        post("/api/offices/${id}/bookings/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(IntegrationTestUtil.asJsonString(info))
                .header("Authorization", "Bearer ${token}")
    }

    void "it should return not authorized when token is not provided"() {
        given:
        def info = BookingInformation.of(
                10,
                LocalDateTime.of(2018, 12, 8, 14, 0),
                LocalDateTime.of(2018, 12, 8, 15, 0)
        )

        when:
        def response = mockMvc.perform(post("/api/offices/1/bookings/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(IntegrationTestUtil.asJsonString(info)))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return bad request when office id provided is not uuid format"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def info = [
                'startTime'        : '2018-12-08T14:00:00',
                'endTime'          : '2018-12-08T15:00:00',
                'attendeesQuantity': 10,
        ]

        when:
        def response = mockMvc.perform(createBookingRequest("-1", info, token.getToken()))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INVALID_OFFICE_ID',
                            'message': 'Office id provided has a wrong format',
                    ]
            ]
        }
    }

    void "it should return not found when there is no office with id provided"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def officeId = new OfficeId()
        def info = [
                'startTime'        : '2018-12-08T14:00:00',
                'endTime'          : '2018-12-08T15:00:00',
                'attendeesQuantity': 10,
        ]

        when:
        def response = mockMvc.perform(createBookingRequest(officeId.toString(), info, token.getToken()))
                .andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'NOT_FOUND',
                            'error'  : 'OFFICE_NOT_FOUND',
                            'message': 'There is no office with id provided',
                    ]
            ]
        }
    }

    void "it should return bad request when office is deleted"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def office = new OfficeBuilder().build()
        office.delete()
        officeRepo.store(office)
        def info = [
                'startTime'        : '2018-12-08T14:00:00',
                'endTime'          : '2018-12-08T15:00:00',
                'attendeesQuantity': 10,
        ]

        when:
        def response = mockMvc.perform(createBookingRequest(office.id().toString(), info, token.getToken()))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'OFFICE_IS_DELETED',
                            'message': 'Office can\'t be booked because is deleted',
                    ]
            ]
        }
    }

    void "it should return bad request when schedule time provided is invalid"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def office = new OfficeBuilder().build()
        officeRepo.store(office)

        when:
        def response = mockMvc.perform(createBookingRequest(office.id().toString(), info, token.getToken()))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INVALID_SCHEDULE_TIME',
                            'message': 'Schedule time provided is invalid: endTime must be after startTime and both should be an exact hour without minutes',
                    ]
            ]
        }

        where:
        info << [
                [
                        'startTime'        : '2018-12-08T14:00:00',
                        'endTime'          : '2018-12-08T12:00:00', // end time is one hour before start time
                        'attendeesQuantity': 10,
                ],
                [
                        'startTime'        : '2018-12-08T14:30:00', // start time is not an exact hour
                        'endTime'          : '2018-12-08T15:00:00',
                        'attendeesQuantity': 10,
                ],
                [
                        'startTime'        : '2018-12-08T14:00:00',
                        'endTime'          : '2018-12-07T15:00:00', // end time is one day before start time
                        'attendeesQuantity': 10,
                ]
        ]
    }

    void "it should return bad request when schedule time provided has an invalid format"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def office = new OfficeBuilder().build()
        officeRepo.store(office)

        when:
        def info = [
                'startTime'        : startTime,
                'endTime'          : endTime,
                'attendeesQuantity': 10,
        ]
        def response = mockMvc.perform(createBookingRequest(office.id().toString(), info, token.getToken()))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors as Set == expectedErrors as Set
        }

        where:
        startTime             | endTime               || expectedErrors
        "2018-12-08T14:00:00" | "asdad"               || [['code': 'INVALID', 'error': 'INVALID_ENDTIME', 'message': 'Input type is invalid',]]
        "asds"                | "2018-12-08T14:00:00" || [['code': 'INVALID', 'error': 'INVALID_STARTTIME', 'message': 'Input type is invalid',]]
    }

    void "it should return bad request when schedule time proposed is not available for shared office"() {
        given: 'Two bookings at the same hour for a shared office with 2 tables quantity'
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def office = new OfficeBuilder()
                .withPrivacy(new SharedOffice(2, 5))
                .build()
        officeRepo.store(office).get()
        def booking1 = new BookingBuilder()
                .withOffice(office)
                .withStartTime(ZonedDateTime.of(2018, 12, 8, 14, 0, 0, 0, ZoneId.of("America/Argentina/Buenos_Aires")))
                .build()
        def booking2 = new BookingBuilder()
                .withOffice(office)
                .withStartTime(ZonedDateTime.of(2018, 12, 8, 14, 0, 0, 0, ZoneId.of("America/Argentina/Buenos_Aires")))
                .build()
        bookingRepo.store(booking1).get()
        bookingRepo.store(booking2).get()

        when: 'Tries to book another table from 14 to 15'
        def info = [
                'startTime'        : '2018-12-08T14:00:00',
                'endTime'          : '2018-12-08T15:00:00',
                'attendeesQuantity': 10,
        ]
        def response = mockMvc.perform(createBookingRequest(office.id().toString(), info, token.getToken()))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'OFFICE_IS_NOT_AVAILABLE',
                            'message': 'Office is not available at schedule time specified',
                    ]
            ]
        }
    }

    void "it should return bad request when schedule time proposed is not available for private office"() {
        given: 'Two bookings at the same hour for a shared office with 2 tables quantity'
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def office = new OfficeBuilder()
                .withPrivacy(new PrivateOffice(10))
                .build()
        officeRepo.store(office).get()
        def booking1 = new BookingBuilder()
                .withOffice(office)
                .withStartTime(ZonedDateTime.of(
                        2018, 12, 8,
                        14, 0, 0, 0,
                        ZoneId.of("America/Argentina/Buenos_Aires")
                )).build()
        bookingRepo.store(booking1).get()

        when: 'Tries to book another table from 14 to 15'
        def info = [
                'startTime'        : '2018-12-08T14:00:00',
                'endTime'          : '2018-12-08T15:00:00',
                'attendeesQuantity': 10,
        ]
        def response = mockMvc.perform(createBookingRequest(office.id().toString(), info, token.getToken()))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'OFFICE_IS_NOT_AVAILABLE',
                            'message': 'Office is not available at schedule time specified',
                    ]
            ]
        }
    }

    void "it should return created when booking is created successfully"() {
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def office = new OfficeBuilder()
                .withPrivacy(new PrivateOffice(10))
                .build()
        officeRepo.store(office).get()
        def booking1 = new BookingBuilder()
                .withOffice(office)
                .withStartTime(ZonedDateTime.of(2018, 12, 8, 14, 0, 0, 0, ZoneId.of("America/Argentina/Buenos_Aires")))
                .build()
        bookingRepo.store(booking1).get()

        when: 'Tries to book another table from 14 to 15'
        def info = [
                'startTime'        : '2018-12-08T15:00:00',
                'endTime'          : '2018-12-08T16:00:00',
                'attendeesQuantity': 10,
        ]
        def response = mockMvc.perform(createBookingRequest(office.id().toString(), info, token.getToken()))
                .andReturn().response

        then:
        response.status == HttpStatus.CREATED.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data['uri'] ==~ "/api/bookings/.*/"
        }
    }

}

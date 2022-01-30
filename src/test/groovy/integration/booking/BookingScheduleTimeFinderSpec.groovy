package integration.booking

import booking.domain.booking.BookingRepository
import booking.domain.office.OfficeId
import booking.domain.office.OfficeRepository
import booking.factories.BookingBuilder
import booking.factories.OfficeBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import server.WorkfficeApplication
import spock.lang.Specification

import java.time.ZoneId
import java.time.ZonedDateTime

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class BookingScheduleTimeFinderSpec extends Specification {
    @Autowired
    OfficeRepository officeRepo
    @Autowired
    BookingRepository bookingRepo
    @Autowired
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper

    ZoneId timezoneARG = ZoneId.of("America/Argentina/Buenos_Aires")

    void "it should return bad request when office id is not uuid format"() {
        when:
        def response = mockMvc.perform(get("/api/offices/1/booking_scheduled_times/?date=2021-01-01"))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INVALID_OFFICE_ID',
                            'message': 'Office id provided has a wrong format'
                    ]
            ]
        }
    }

    void "it should return bad request when date has an invalid format"() {
        when:
        def response = mockMvc.perform(get("/api/offices/1/booking_scheduled_times/?date=01-01-2021"))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INVALID_DATE',
                            'message': 'The date format is invalid, must be aaaa-mm-dd'
                    ]
            ]
        }
    }

    void "it should return not found when there is no office with id provided"() {
        given:
        def officeId = new OfficeId()

        when:
        def response = mockMvc.perform(get("/api/offices/${officeId}/booking_scheduled_times/?date=2021-01-01"))
                .andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'NOT_FOUND',
                            'error'  : 'OFFICE_NOT_FOUND',
                            'message': 'There is no office with id provided'
                    ]
            ]
        }
    }

    void "it should return booking schedule times for office and date specified"() {
        given:
        def office = new OfficeBuilder().build()
        def office2 = new OfficeBuilder().build()
        officeRepo.store(office)
        officeRepo.store(office2)
        def booking1 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 1, 1,
                        14, 0, 0, 0,
                        timezoneARG))
                .withOffice(office)
                .build()
        def booking2 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 1, 1,
                        10, 0, 0, 0,
                        timezoneARG))
                .withEndTime(ZonedDateTime.of(
                        2021, 1, 1,
                        12, 0, 0, 0,
                        timezoneARG))
                .withOffice(office)
                .build()
        def booking3 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 1, 1,
                        17, 0, 0, 0,
                        timezoneARG))
                .withOffice(office)
                .build()
        def booking4 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 1, 1,
                        14, 0, 0, 0,
                        timezoneARG))
                .withOffice(office2)
                .build()
        bookingRepo.store(booking1)
        bookingRepo.store(booking2)
        bookingRepo.store(booking3)
        bookingRepo.store(booking4)

        when:
        def response = mockMvc.perform(get("/api/offices/${office.id()}/booking_scheduled_times/?date=2021-01-01"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data.size() == 3
            it.data as Set == [
                    [
                            'date'     : '2021-01-01',
                            'startTime': '14:00:00',
                            'endTime'  : '15:00:00',
                    ],
                    [
                            'date'     : '2021-01-01',
                            'startTime': '10:00:00',
                            'endTime'  : '12:00:00',
                    ],
                    [
                            'date'     : '2021-01-01',
                            'startTime': '17:00:00',
                            'endTime'  : '18:00:00',
                    ],
            ] as Set
        }
    }
}

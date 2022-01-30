package integration.booking

import authentication.application.dto.token.Authentication
import backoffice.domain.office_branch.OfficeBranchId
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.domain.role.Access
import backoffice.domain.role.Permission
import backoffice.domain.role.Resource
import backoffice.factories.OfficeBranchBuilder
import booking.domain.booking.BookingRepository
import booking.domain.booking.Status
import booking.domain.office.OfficeId
import booking.domain.office.OfficeRepository
import booking.factories.BookingBuilder
import booking.factories.OfficeBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.javafaker.Faker
import controller.AuthUtil
import controller.CollaboratorUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import server.WorkfficeApplication
import spock.lang.Specification

import java.time.ZoneId
import java.time.ZonedDateTime

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class BookingByOfficeFinderSpec extends Specification {
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    OfficeRepository officeRepo
    @Autowired
    BookingRepository bookingRepo
    @Autowired
    AuthUtil authUtil
    @Autowired
    CollaboratorUtil collaboratorUtil
    @Autowired
    ObjectMapper objectMapper
    @Autowired
    MockMvc mockMvc

    Faker faker = Faker.instance()
    ZoneId timezoneARG = ZoneId.of("America/Argentina/Buenos_Aires")

    private static MockHttpServletRequestBuilder getOfficeBookingsRequest(String officeId, String date, Authentication token) {
        get("/api/offices/${officeId}/bookings/?date=${date}")
                .header("Authorization", "Bearer ${token.getToken()}")
    }

    void "it should return not authorized when token is not provided"() {
        when:
        def response = mockMvc.perform(get("/api/offices/1/bookings/?date=2021-12-08"))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return bad request when office id is not uuid format"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(getOfficeBookingsRequest("-1", "2021-12-08", token))
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

    void "it should return bad request when specified date has an invalid format"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        var officeId = new OfficeId()

        when:
        def response = mockMvc.perform(getOfficeBookingsRequest(officeId.toString(), "12-08-2021", token))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INVALID_DATE',
                            'message': 'The date format is invalid, must be aaaa-mm-dd',
                    ]
            ]
        }
    }

    void "it should return not found when there is no office with id specified"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        var officeId = new OfficeId()

        when:
        def response = mockMvc.perform(getOfficeBookingsRequest(officeId.toString(), "2021-12-08", token))
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

    void "it should return forbidden when auth user is not owner of office branch"() {
        given:
        var office = new OfficeBuilder().build()
        var booking = new BookingBuilder().withOffice(office).build()
        var officeBranch = new OfficeBranchBuilder()
                .withId(OfficeBranchId.fromString(office.officeBranchId()))
                .build()
        officeRepo.store(office)
        bookingRepo.store(booking)
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(getOfficeBookingsRequest(office.id().toString(), "2021-12-08", token))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'BOOKING_FORBIDDEN',
                            'message': 'You don\'t have access to office bookings'
                    ]
            ]
        }
    }

    void "it should return forbidden when collaborator has no read access to office branch bookings"() {
        given:
        var office = new OfficeBuilder().build()
        var booking = new BookingBuilder().withOffice(office).build()
        var officeBranch = new OfficeBranchBuilder()
                .withId(OfficeBranchId.fromString(office.officeBranchId()))
                .build()
        officeRepo.store(office)
        bookingRepo.store(booking)
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and:
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.READ, Resource.REPORT)] as Set
        )
        def token = authUtil.createAndLoginUser(collaborator.email(), "1234")

        when:
        def response = mockMvc.perform(getOfficeBookingsRequest(office.id().toString(), "2021-12-08", token))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'BOOKING_FORBIDDEN',
                            'message': 'You don\'t have access to office bookings'
                    ]
            ]
        }
    }

    void "it should return ok with bookings related with office in specified date"() {
        given:
        var office = new OfficeBuilder()
                .withPrice(1500)
                .build()
        var officeBranch = new OfficeBranchBuilder()
                .withId(OfficeBranchId.fromString(office.officeBranchId()))
                .build()
        officeRepo.store(office)
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and:
        var booking = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 9,
                        16, 0, 0, 0,
                        timezoneARG))
                .withStatus(Status.SCHEDULED)
                .withOffice(office).build()
        var booking2 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 8,
                        12, 0, 0, 0,
                        timezoneARG))
                .withStatus(Status.PENDING)
                .withOffice(office).build()
        var booking3 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 8,
                        16, 0, 0, 0,
                        timezoneARG))
                .withStatus(Status.SCHEDULED)
                .withOffice(office).build()
        bookingRepo.store(booking)
        bookingRepo.store(booking2)
        bookingRepo.store(booking3)
        and:
        def token = authUtil.createAndLoginUser(officeBranch.owner().email(), "1234")

        when:
        def response = mockMvc.perform(getOfficeBookingsRequest(office.id().toString(), "2021-12-08", token))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and: "It returns only scheduled bookings at specified date"
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data.size() == 1
            it.data as Set == [
                    [
                            'id'               : booking3.toResponse().id,
                            'status'           : 'SCHEDULED',
                            'attendeesQuantity': booking3.toResponse().attendeesQuantity,
                            'totalAmount'      : 1500f,
                            'created'          : booking3.toResponse().created.toString() + ":00",
                            'startTime'        : booking3.toResponse().startTime.toString() + ":00",
                            'endTime'          : booking3.toResponse().endTime.toString() + ":00",
                            'payment'          : [
                                    'id'               : booking3.toResponse().payment.id,
                                    'externalId'       : '12-external',
                                    'transactionAmount': 1500f,
                                    'providerFee'      : 8f,
                                    'currency'         : 'ARS',
                                    'paymentMethodId'  : 'visa',
                                    'paymentTypeId'    : 'credit_card',
                            ],
                            'officeId'         : booking3.toResponse().officeId,
                            'officeName'       : booking3.toResponse().officeName,
                            'officeBranchId'   : booking3.toResponse().officeBranchId,
                    ]
            ] as Set
        }
    }

    void "it should return ok when collaborator has read access to bookings"() {
        given:
        var office = new OfficeBuilder()
                .withPrice(1500)
                .build()
        var officeBranch = new OfficeBranchBuilder()
                .withId(OfficeBranchId.fromString(office.officeBranchId()))
                .build()
        officeRepo.store(office)
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and:
        var booking = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 9,
                        16, 0, 0, 0,
                        timezoneARG))
                .withStatus(Status.SCHEDULED)
                .withOffice(office).build()
        var booking2 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 8,
                        12, 0, 0, 0,
                        timezoneARG))
                .withStatus(Status.SCHEDULED)
                .withOffice(office).build()
        var booking3 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2021, 12, 8,
                        16, 0, 0, 0,
                        timezoneARG))
                .withStatus(Status.SCHEDULED)
                .withOffice(office).build()
        bookingRepo.store(booking)
        bookingRepo.store(booking2)
        bookingRepo.store(booking3)
        and:
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.READ, Resource.BOOKING)] as Set
        )
        def token = authUtil.createAndLoginUser(collaborator.email(), "1234")

        when:
        def response = mockMvc.perform(getOfficeBookingsRequest(office.id().toString(), "2021-12-08", token))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and: "It returns only scheduled bookings at specified date"
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data.size() == 2
            it.data as Set == [
                    [
                            'id'               : booking2.toResponse().id,
                            'status'           : 'SCHEDULED',
                            'attendeesQuantity': booking2.toResponse().attendeesQuantity,
                            'totalAmount'      : 1500f,
                            'created'          : booking2.toResponse().created.toString() + ":00",
                            'startTime'        : booking2.toResponse().startTime.toString() + ":00",
                            'endTime'          : booking2.toResponse().endTime.toString() + ":00",
                            'payment'          : [
                                    'id'               : booking2.toResponse().payment.id,
                                    'externalId'       : '12-external',
                                    'transactionAmount': 1500f,
                                    'providerFee'      : 8f,
                                    'currency'         : 'ARS',
                                    'paymentMethodId'  : 'visa',
                                    'paymentTypeId'    : 'credit_card',
                            ],
                            'officeId'         : booking2.toResponse().officeId,
                            'officeName'       : booking2.toResponse().officeName,
                            'officeBranchId'   : booking2.toResponse().officeBranchId,
                    ],
                    [
                            'id'               : booking3.toResponse().id,
                            'status'           : 'SCHEDULED',
                            'attendeesQuantity': booking3.toResponse().attendeesQuantity,
                            'totalAmount'      : 1500f,
                            'created'          : booking3.toResponse().created.toString() + ":00",
                            'startTime'        : booking3.toResponse().startTime.toString() + ":00",
                            'endTime'          : booking3.toResponse().endTime.toString() + ":00",
                            'payment'          : [
                                    'id'               : booking3.toResponse().payment.id,
                                    'externalId'       : '12-external',
                                    'transactionAmount': 1500f,
                                    'providerFee'      : 8f,
                                    'currency'         : 'ARS',
                                    'paymentMethodId'  : 'visa',
                                    'paymentTypeId'    : 'credit_card',
                            ],
                            'officeId'         : booking3.toResponse().officeId,
                            'officeName'       : booking3.toResponse().officeName,
                            'officeBranchId'   : booking3.toResponse().officeBranchId,
                    ]
            ] as Set
        }
    }
}

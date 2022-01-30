package integration.office_branch

import backoffice.domain.office_branch.OfficeBranchId
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.domain.role.Access
import backoffice.domain.role.Permission
import backoffice.domain.role.Resource
import backoffice.factories.OfficeBranchBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.javafaker.Faker
import controller.AuthUtil
import controller.CollaboratorUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import report.domain.Booking
import report.domain.BookingRepository
import server.WorkfficeApplication
import spock.lang.Specification

import java.time.LocalDate

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class OfficeBranchReportsSpec extends Specification {
    @Autowired
    BookingRepository bookingRepo
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    AuthUtil authUtil
    @Autowired
    CollaboratorUtil collaboratorUtil
    @Autowired
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper
    @Autowired
    MongoTemplate mongoTemplate

    Faker faker = Faker.instance()

    def cleanup() {
        var collection = mongoTemplate.getCollection("bookings")
        collection.drop()
    }

    void "it should return not authorized when token is not provided"() {
        when:
        def response = mockMvc.perform(get(endpoint))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()

        where:
        endpoint << [
                '/api/office_branch_reports/1/total_amount_per_office/?month=SEPTEMBER',
                '/api/office_branch_reports/1/total_bookings_per_office/?month=SEPTEMBER',
                '/api/office_branch_reports/1/total_amount_per_month/?year=2021',
        ]
    }

    void "it should return bad request when office branch id is invalid"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(get(endpoint)
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INVALID_OFFICE_BRANCH_ID',
                            'message': 'The office branch id specified is invalid'
                    ]
            ]
        }

        where:
        endpoint << [
                '/api/office_branch_reports/-1/total_amount_per_office/?month=SEPTEMBER',
                '/api/office_branch_reports/-1/total_bookings_per_office/?month=SEPTEMBER',
                '/api/office_branch_reports/-1/total_amount_per_month/?year=2021',
        ]
    }

    void "it should return not found when there is no office branch with id specified"() {
        given:
        String officeBranchId = new OfficeBranchId().toString()
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(get(endpoint(officeBranchId))
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'NOT_FOUND',
                            'error'  : 'OFFICE_BRANCH_NOT_FOUND',
                            'message': 'There is no office branch with id provided'
                    ]
            ]
        }

        where:
        endpoint << [
                (id) -> "/api/office_branch_reports/${id}/total_amount_per_office/?month=SEPTEMBER",
                (id) -> "/api/office_branch_reports/${id}/total_bookings_per_office/?month=SEPTEMBER",
                (id) -> "/api/office_branch_reports/${id}/total_amount_per_month/?year=2021",
        ]
    }

    void "it should return forbidden when auth user is not owner of office branch reports"() {
        given:
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(get(endpoint(officeBranch.id().toString()))
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'OFFICE_BRANCH_FORBIDDEN',
                            'message': 'You do not have access to office branch reports'
                    ]
            ]
        }

        where:
        endpoint << [
                (id) -> "/api/office_branch_reports/${id}/total_amount_per_office/?month=SEPTEMBER",
                (id) -> "/api/office_branch_reports/${id}/total_bookings_per_office/?month=SEPTEMBER",
                (id) -> "/api/office_branch_reports/${id}/total_amount_per_month/?year=2021",
        ]
    }

    void "it should return forbidden when auth user is not a collaborator with read access to reports"() {
        given:
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and:
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.WRITE, Resource.OFFICE)] as Set
        )
        def token = authUtil.createAndLoginUser(collaborator.email(), "1234")

        when:
        def response = mockMvc.perform(get(endpoint(officeBranch.id().toString()))
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'OFFICE_BRANCH_FORBIDDEN',
                            'message': 'You do not have access to office branch reports'
                    ]
            ]
        }

        where:
        endpoint << [
                (id) -> "/api/office_branch_reports/${id}/total_amount_per_office/?month=SEPTEMBER",
                (id) -> "/api/office_branch_reports/${id}/total_bookings_per_office/?month=SEPTEMBER",
                (id) -> "/api/office_branch_reports/${id}/total_amount_per_month/?year=2021",
        ]
    }

    void "it should return ok with reports when auth user is owner of office branch reports"() {
        given:
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and:
        def token = authUtil.createAndLoginUser(officeBranch.owner().email(), "1234")
        and:
        def booking1 = Booking.create(
                "1",
                officeBranch.id().toString(),
                "1",
                8000f,
                LocalDate.of(2021, 9, 14)
        )
        def booking2 = Booking.create(
                "2",
                officeBranch.id().toString(),
                "2",
                100f,
                LocalDate.of(2021, 11, 8)
        )
        def booking3 = Booking.create(
                "3",
                officeBranch.id().toString(),
                "2",
                100f,
                LocalDate.of(2021, 9, 21)
        )
        bookingRepo.store(booking1)
        bookingRepo.store(booking2)
        bookingRepo.store(booking3)

        when:
        def response = mockMvc.perform(get(endpoint(officeBranch.id().toString()))
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == expectedResponse
        }

        where:
        endpoint << [
                (id) -> "/api/office_branch_reports/${id}/total_amount_per_office/?month=SEPTEMBER",
                (id) -> "/api/office_branch_reports/${id}/total_bookings_per_office/?month=SEPTEMBER",
                (id) -> "/api/office_branch_reports/${id}/total_amount_per_month/?year=2021",
        ]
        expectedResponse << [
                [
                        ['officeId': '1', 'month': 'SEPTEMBER', 'totalAmount': 8000f],
                        ['officeId': '2', 'month': 'SEPTEMBER', 'totalAmount': 100f],
                ],
                [
                        ['officeId': '2', 'month': 'SEPTEMBER', 'totalBookings': 1],
                        ['officeId': '1', 'month': 'SEPTEMBER', 'totalBookings': 1],
                ],
                [
                        ['year': 2021, 'month': 'NOVEMBER', 'totalAmount': 100f],
                        ['year': 2021, 'month': 'SEPTEMBER', 'totalAmount': 8100f],
                ],
        ]
    }

    void "it should return ok with reports when auth user is collaborator with read access to reports"() {
        given:
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and:
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [
                        Permission.create(Access.READ, Resource.REPORT),
                        Permission.create(Access.WRITE, Resource.COLLABORATOR),
                ] as Set
        )
        def token = authUtil.createAndLoginUser(collaborator.email(), "1234")
        and:
        def booking1 = Booking.create(
                "1",
                officeBranch.id().toString(),
                "1",
                8000f,
                LocalDate.of(2021, 9, 11)
        )
        def booking2 = Booking.create(
                "2",
                officeBranch.id().toString(),
                "2",
                100f,
                LocalDate.of(2021, 11, 8)
        )
        def booking3 = Booking.create(
                "3",
                officeBranch.id().toString(),
                "2",
                100f,
                LocalDate.of(2021, 9, 21)
        )
        bookingRepo.store(booking1)
        bookingRepo.store(booking2)
        bookingRepo.store(booking3)

        when:
        def response = mockMvc.perform(get(endpoint(officeBranch.id().toString()))
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == expectedResponse
        }

        where:
        endpoint << [
                (id) -> "/api/office_branch_reports/${id}/total_amount_per_office/?month=SEPTEMBER",
                (id) -> "/api/office_branch_reports/${id}/total_bookings_per_office/?month=SEPTEMBER",
                (id) -> "/api/office_branch_reports/${id}/total_amount_per_month/?year=2021",
        ]
        expectedResponse << [
                [
                        ['officeId': '1', 'month': 'SEPTEMBER', 'totalAmount': 8000f],
                        ['officeId': '2', 'month': 'SEPTEMBER', 'totalAmount': 100f],
                ],
                [
                        ['officeId': '2', 'month': 'SEPTEMBER', 'totalBookings': 1],
                        ['officeId': '1', 'month': 'SEPTEMBER', 'totalBookings': 1],
                ],
                [
                        ['year': 2021, 'month': 'NOVEMBER', 'totalAmount': 100f],
                        ['year': 2021, 'month': 'SEPTEMBER', 'totalAmount': 8100f],
                ],
        ]
    }
}

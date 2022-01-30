package integration.office

import authentication.application.dto.token.Authentication
import backoffice.application.dto.office_inactivity.InactivityInformation
import backoffice.domain.office.OfficeId
import backoffice.domain.office.OfficeRepository
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.domain.office_inactivity.InactivityRepository
import backoffice.domain.role.Access
import backoffice.domain.role.Permission
import backoffice.domain.role.Resource
import backoffice.factories.InactivityBuilder
import backoffice.factories.OfficeBranchBuilder
import backoffice.factories.OfficeBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.javafaker.Faker
import controller.AuthUtil
import controller.CollaboratorUtil
import controller.IntegrationTestUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import server.WorkfficeApplication
import shared.domain.EventBus
import spock.lang.Specification

import java.time.DayOfWeek
import java.time.LocalDate

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class OfficeInactivitiesUpdaterSpec extends Specification {
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    OfficeRepository officeRepo
    @Autowired
    InactivityRepository inactivityRepo
    @Autowired
    AuthUtil authUtil
    @Autowired
    CollaboratorUtil collaboratorUtil
    @Autowired
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper
    @MockBean
    EventBus eventBus

    Faker faker = Faker.instance()

    private static MockHttpServletRequestBuilder updateOfficeInactivitiesRequest(
            String officeId,
            Authentication token,
            List<Object> infos
    ) {
        put("/api/offices/${officeId}/inactivities/")
                .header("Authorization", "Bearer ${token.getToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(IntegrationTestUtil.asJsonString(infos))
    }

    void "it should return not authorized when token is not provided"() {
        when:
        def response = mockMvc.perform(put("/api/offices/1/inactivities/"))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return bad request when office id provided is not uuid format"() {
        given:
        def info = InactivityInformation.of(
                "RECURRING_DAY",
                DayOfWeek.MONDAY,
                null,
        )
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(updateOfficeInactivitiesRequest("-1", token, [info]))
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

    void "it should return not found when office id provided does not exist"() {
        given:
        def info = InactivityInformation.of(
                "RECURRING_DAY",
                DayOfWeek.MONDAY,
                null,
        )
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(updateOfficeInactivitiesRequest(new OfficeId().toString(), token, [info]))
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

    void "it should return forbidden when auth user is not owner of office"() {
        given:
        def officeBranch = new OfficeBranchBuilder().build()
        def office = new OfficeBuilder().withOfficeBranch(officeBranch).build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        officeRepo.store(office)
        and:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def info = InactivityInformation.of(
                "RECURRING_DAY",
                DayOfWeek.MONDAY,
                null,
        )
        def response = mockMvc.perform(updateOfficeInactivitiesRequest(office.id().toString(), token, [info]))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'OFFICE_FORBIDDEN',
                            'message': 'Forbidden you do not have access to office',
                    ]
            ]
        }
    }

    void "it should return forbidden when collaborator does not have write access to offices"() {
        given:
        def officeBranch = new OfficeBranchBuilder().build()
        def office = new OfficeBuilder().withOfficeBranch(officeBranch).build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        officeRepo.store(office)
        and:
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.WRITE, Resource.ROLE)] as Set
        )
        def token = authUtil.createAndLoginUser(collaborator.email(), "1234")

        when:
        def info = InactivityInformation.of(
                "RECURRING_DAY",
                DayOfWeek.MONDAY,
                null,
        )
        def response = mockMvc.perform(updateOfficeInactivitiesRequest(office.id().toString(), token, [info]))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'OFFICE_FORBIDDEN',
                            'message': 'Forbidden you do not have access to office',
                    ]
            ]
        }
    }

    void "it should return bad request when there is at least one inactivity with invalid information"() {
        given:
        def officeBranch = new OfficeBranchBuilder().build()
        def office = new OfficeBuilder().withOfficeBranch(officeBranch).build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        officeRepo.store(office)
        and:
        def token = authUtil.createAndLoginUser(officeBranch.owner().email(), "1234")

        when:
        def info = InactivityInformation.of(
                "SPECIFIC_DATE",
                DayOfWeek.MONDAY,
                null,
        )
        def response = mockMvc.perform(updateOfficeInactivitiesRequest(office.id().toString(), token, [info]))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INACTIVITY_TYPE_MISMATCH_WITH_DATE',
                            'message': 'The inactivity does not match with the date',
                    ]
            ]
        }
    }

    void "it should return accepted when update office inactivities successfully"() {
        given:
        def officeBranch = new OfficeBranchBuilder().build()
        def office = new OfficeBuilder().withOfficeBranch(officeBranch).build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        officeRepo.store(office)
        and:
        def inactivity1 = new InactivityBuilder()
                .withSpecificDate(LocalDate.of(2021, 1, 12))
                .withOffice(office)
                .build()
        def inactivity2 = new InactivityBuilder()
                .withDayOfWeek(DayOfWeek.MONDAY)
                .withOffice(office)
                .build()
        def inactivity3 = new InactivityBuilder()
                .withDayOfWeek(DayOfWeek.FRIDAY)
                .withOffice(office)
                .build()
        inactivityRepo.store(inactivity1)
        inactivityRepo.store(inactivity2)
        inactivityRepo.store(inactivity3)
        and:
        def token = authUtil.createAndLoginUser(officeBranch.owner().email(), "1234")

        when:
        def info = [
                'type'                 : "RECURRING_DAY",
                'dayOfWeek'            : DayOfWeek.MONDAY,
                'specificInactivityDay': null,
        ]
        def info2 = [
                'type'                 : "SPECIFIC_DATE",
                'dayOfWeek'            : null,
                'specificInactivityDay': "2021-01-12",
        ]
        def response = mockMvc.perform(updateOfficeInactivitiesRequest(office.id().toString(), token, [info, info2]))
                .andReturn().response

        then:
        response.status == HttpStatus.ACCEPTED.value()
        and:
        def inactivities = inactivityRepo.findAllByOffice(office)
        inactivities.size() == 2
    }

    void "it should return accepted when update office inactivities successfully with a collaborator with right perms"() {
        given:
        def officeBranch = new OfficeBranchBuilder().build()
        def office = new OfficeBuilder().withOfficeBranch(officeBranch).build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        officeRepo.store(office)
        and:
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.WRITE, Resource.OFFICE)] as Set
        )
        def token = authUtil.createAndLoginUser(collaborator.email(), "1234")

        when:
        def info = InactivityInformation.of(
                "RECURRING_DAY",
                DayOfWeek.WEDNESDAY,
                null,
        )
        def response = mockMvc.perform(updateOfficeInactivitiesRequest(office.id().toString(), token, [info]))
                .andReturn().response

        then:
        response.status == HttpStatus.ACCEPTED.value()
        and:
        def inactivities = inactivityRepo.findAllByOffice(office)
        inactivities.size() == 1
    }
}

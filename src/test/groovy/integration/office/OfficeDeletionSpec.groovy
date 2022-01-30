package integration.office

import authentication.application.dto.token.Authentication
import backoffice.domain.office.OfficeId
import backoffice.domain.office.OfficeRepository
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.domain.role.Access
import backoffice.domain.role.Permission
import backoffice.domain.role.Resource
import backoffice.factories.OfficeBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.javafaker.Faker
import controller.AuthUtil
import controller.CollaboratorUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import server.WorkfficeApplication
import shared.domain.EventBus
import spock.lang.Specification

import java.time.Clock
import java.time.LocalDate

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class OfficeDeletionSpec extends Specification {
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    OfficeRepository officeRepo
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

    private static MockHttpServletRequestBuilder deleteOfficeRequest(
            String id,
            Authentication token
    ) {
        delete("/api/offices/${id}/")
                .header("Authorization", "Bearer ${token.getToken()}")
    }

    void "it should return not authorized when token is not provided"() {
        when:
        def response = mockMvc.perform(delete("/api/offices/1/"))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return bad request when office id is not uuid format"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(deleteOfficeRequest("-1", token))
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

        when:
        def response = mockMvc.perform(deleteOfficeRequest(officeId.toString(), token))
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

    void "it should return not found when office is already deleted"() {
        given: 'An office that belong to an office branch'
        def office = new OfficeBuilder().build()
        office.delete(LocalDate.now(Clock.systemUTC()).minusDays(1));
        officeHolderRepo.store(office.officeBranch().owner())
        officeBranchRepo.store(office.officeBranch())
        officeRepo.store(office)
        and: 'An authenticated user that is the owner of the office branch'
        def token = authUtil.createAndLoginUser(office.officeBranch().owner().email(), "1234")

        when:
        def response = mockMvc.perform(deleteOfficeRequest(office.id().toString(), token))
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

    void "it should return forbidden when auth user is not owner of office branch that contains office"() {
        given: 'An office that belong to an office branch'
        def office = new OfficeBuilder().build()
        officeHolderRepo.store(office.officeBranch().owner())
        officeBranchRepo.store(office.officeBranch())
        officeRepo.store(office)
        and: 'An authenticated user that is not the owner of the office branch'
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(deleteOfficeRequest(office.id().toString(), token))
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

    void "it should return forbidden when collaborator does not have write access to office"() {
        given: 'An office that belong to an office branch'
        def office = new OfficeBuilder().build()
        officeHolderRepo.store(office.officeBranch().owner())
        officeBranchRepo.store(office.officeBranch())
        officeRepo.store(office)
        and: 'An authenticated collaborator that not has write access to office resource'
        def collaborator = collaboratorUtil.createCollaborator(
                office.officeBranch(),
                [Permission.create(Access.READ, Resource.OFFICE)] as Set
        )
        def token = authUtil.createAndLoginUser(collaborator.email(), "1234")

        when:
        def response = mockMvc.perform(deleteOfficeRequest(office.id().toString(), token))
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

    void "it should return accepted when office is deleted successfully"() {
        given: 'An office that belong to an office branch'
        def office = new OfficeBuilder().build()
        officeHolderRepo.store(office.officeBranch().owner())
        officeBranchRepo.store(office.officeBranch())
        officeRepo.store(office)
        and: 'An authenticated user that is the owner of the office branch'
        def token = authUtil.createAndLoginUser(office.officeBranch().owner().email(), "1234")

        when:
        def response = mockMvc.perform(deleteOfficeRequest(office.id().toString(), token))
                .andReturn().response

        then:
        response.status == HttpStatus.ACCEPTED.value()
    }

    void "it should return accepted when collaborator with right access deletes office successfully"() {
        given: 'An office that belong to an office branch'
        def office = new OfficeBuilder().build()
        officeHolderRepo.store(office.officeBranch().owner())
        officeBranchRepo.store(office.officeBranch())
        officeRepo.store(office)
        and: 'An authenticated collaborator with write access to office resource'
        def collaborator = collaboratorUtil.createCollaborator(
                office.officeBranch(),
                [Permission.create(Access.WRITE, Resource.OFFICE)] as Set
        )
        def token = authUtil.createAndLoginUser(collaborator.email(), "1234")

        when:
        def response = mockMvc.perform(deleteOfficeRequest(office.id().toString(), token))
                .andReturn().response

        then:
        response.status == HttpStatus.ACCEPTED.value()
    }
}

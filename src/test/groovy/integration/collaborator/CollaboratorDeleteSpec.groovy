package integration.collaborator

import backoffice.domain.collaborator.CollaboratorId
import backoffice.domain.collaborator.CollaboratorRepository
import backoffice.domain.office_branch.OfficeBranch
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.domain.role.Access
import backoffice.domain.role.Permission
import backoffice.domain.role.Resource
import backoffice.factories.CollaboratorBuilder
import backoffice.factories.OfficeBranchBuilder
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
import server.WorkfficeApplication
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class CollaboratorDeleteSpec extends Specification {
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    CollaboratorRepository collaboratorRepo
    @Autowired
    AuthUtil authUtil
    @Autowired
    CollaboratorUtil collaboratorUtil
    @Autowired
    ObjectMapper objectMapper
    @Autowired
    MockMvc mockMvc

    Faker faker = Faker.instance()

    OfficeBranch createOfficeBranch() {
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        return officeBranch
    }

    void "it should return not authorized when token is not provided"() {
        when:
        def response = mockMvc.perform(delete("/api/collaborators/1/"))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return bad request when collaborator id is invalid"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(delete("/api/collaborators/-1/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INVALID_COLLABORATOR_ID',
                            'message': 'The collaborator id specified has an invalid format',
                    ]
            ]
        }

    }

    void "it should return not found when there is no colllaborator with id specified"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def collaboratorId = new CollaboratorId()

        when:
        def response = mockMvc.perform(delete("/api/collaborators/${collaboratorId}/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'NOT_FOUND',
                            'error'  : 'COLLABORATOR_NOT_FOUND',
                            'message': 'There is no collaborator with id specified',
                    ]
            ]
        }
    }

    void "it should return forbidden whe auth user does not have access"() {
        given:
        def officeBranch = createOfficeBranch()
        def collaborator = new CollaboratorBuilder()
                .withOfficeBranch(officeBranch)
                .build()
        collaboratorRepo.store(collaborator)
        and: "An authenticated user that is not the owner of the collaborator"
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(delete("/api/collaborators/${collaborator.id()}/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'COLLABORATOR_FORBIDDEN',
                            'message': 'You can\'t access to this collaborator',
                    ]
            ]
        }
    }

    void "it should return forbidden when collaborator has no collaborator write access"() {
        given:
        def officeBranch = createOfficeBranch()
        def collaboratorToDelete = new CollaboratorBuilder()
                .withOfficeBranch(officeBranch)
                .build()
        collaboratorRepo.store(collaboratorToDelete)
        and: "An authenticated with insufficient permissions"
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.READ, Resource.COLLABORATOR)] as Set
        )
        def token = authUtil.createAndLoginUser(collaborator.email(), "1234")

        when:
        def response = mockMvc.perform(delete("/api/collaborators/${collaboratorToDelete.id()}/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'COLLABORATOR_FORBIDDEN',
                            'message': 'You can\'t access to this collaborator'
                    ]
            ]
        }
    }

    void "it should return ok when collaborator is deleted successfully by the office branch owner"() {
        given:
        def officeBranch = createOfficeBranch()
        def collaborator = new CollaboratorBuilder()
                .withOfficeBranch(officeBranch)
                .build()
        collaboratorRepo.store(collaborator)
        def token = authUtil.createAndLoginUser(officeBranch.owner().email(), "1234")

        when:
        def response = mockMvc.perform(delete("/api/collaborators/${collaborator.id()}/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        def collaboratorUpdated = collaboratorRepo.findById(collaborator.id()).get()
        !collaboratorUpdated.isActive()
    }

    void "it should return ok when collaborator is deleted successfully by a collaborator with write access to collaborator"() {
        given:
        def officeBranch = createOfficeBranch()
        def collaboratorToDelete = new CollaboratorBuilder()
                .withOfficeBranch(officeBranch)
                .build()
        collaboratorRepo.store(collaboratorToDelete)
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.WRITE, Resource.COLLABORATOR)] as Set
        )
        def token = authUtil.createAndLoginUser(collaborator.email(), "1234")

        when:
        def response = mockMvc.perform(delete("/api/collaborators/${collaboratorToDelete.id()}/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        def collaboratorUpdated = collaboratorRepo.findById(collaboratorToDelete.id()).get()
        !collaboratorUpdated.isActive()
    }
}

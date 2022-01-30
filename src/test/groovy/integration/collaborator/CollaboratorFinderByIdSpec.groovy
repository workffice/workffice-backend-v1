package integration.collaborator

import backoffice.domain.collaborator.CollaboratorId
import backoffice.domain.collaborator.CollaboratorRepository
import backoffice.domain.collaborator.Status
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.domain.role.Access
import backoffice.domain.role.Permission
import backoffice.domain.role.Resource
import backoffice.factories.CollaboratorBuilder
import backoffice.factories.OfficeBranchBuilder
import com.fasterxml.jackson.databind.ObjectMapper
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class CollaboratorFinderByIdSpec extends Specification {
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
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper

    void "it should return unauthorized when token is not provided"() {
        given:
        def collaboratorId = new CollaboratorId()

        when:
        def response = mockMvc
                .perform(get("/api/collaborators/${collaboratorId}/"))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return bad request when collaborator id is not uuid format"() {
        given:
        def token = authUtil.createAndLoginUser("mm@mail.com", "1234")

        when:
        def response = mockMvc
                .perform(get("/api/collaborators/-1/")
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
                            'message': 'The collaborator id specified has an invalid format'
                    ]
            ]
        }
    }

    void "it should return not found when collaborator id specified does not exist"() {
        given:
        def nonExistentCollaboratorId = new CollaboratorId()
        def token = authUtil.createAndLoginUser("nn@mail.com", "1234")

        when:
        def response = mockMvc
                .perform(get("/api/collaborators/${nonExistentCollaboratorId}/")
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
                            'message': 'There is no collaborator with id specified'
                    ]
            ]
        }
    }

    void "it should return forbidden when auth user is not owner of office branch"() {
        given: 'An office owner with a single collaborator'
        def officeBranch = new OfficeBranchBuilder().build()
        def collaborator = new CollaboratorBuilder()
                .withOfficeBranch(officeBranch)
                .build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        collaboratorRepo.store(collaborator)
        and: 'An authenticated user that is not the office owner'
        def token = authUtil.createAndLoginUser("another@mail.com", "1234")

        when:
        def response = mockMvc
                .perform(get("/api/collaborators/${collaborator.id()}/")
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
                            'message': "You can't access to this collaborator"
                    ]
            ]
        }
    }

    void "it should return forbidden when auth user has no collaborator read access"() {
        given: 'An office owner with a single collaborator'
        def officeBranch = new OfficeBranchBuilder().build()
        def collaborator = new CollaboratorBuilder()
                .withOfficeBranch(officeBranch)
                .build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        collaboratorRepo.store(collaborator)
        and: 'An active collaborator without read access to collaborators'
        def collaboratorWithoutReadCollaboratorPerm = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.WRITE, Resource.OFFICE)] as Set
        )
        def token = authUtil.createAndLoginUser(
                collaboratorWithoutReadCollaboratorPerm.email(),
                "1234"
        )

        when:
        def response = mockMvc
                .perform(get("/api/collaborators/${collaborator.id()}/")
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
                            'message': "You can't access to this collaborator"
                    ]
            ]
        }
    }

    void "it should return ok when collaborator is found successfully"() {
        given: 'An office owner with a single collaborator'
        def officeBranch = new OfficeBranchBuilder().build()
        def collaborator = new CollaboratorBuilder()
                .withStatus(Status.PENDING)
                .withOfficeBranch(officeBranch)
                .build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        collaboratorRepo.store(collaborator)
        and: 'Owner of office branch authenticated'
        def token = authUtil.createAndLoginUser(
                officeBranch.owner().email(),
                "1234"
        )

        when:
        def response = mockMvc
                .perform(get("/api/collaborators/${collaborator.id()}/")
                        .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == [
                    'id'     : collaborator.toResponse().id,
                    'email'  : collaborator.toResponse().email,
                    'name'   : collaborator.toResponse().name,
                    'status' : collaborator.toResponse().status,
                    'created': collaborator.toResponse().created.toString()
            ]
        }
    }

    void "it should return ok when auth user is collaborator with right access"() {
        given: 'An office owner with a single collaborator'
        def officeBranch = new OfficeBranchBuilder().build()
        def collaborator = new CollaboratorBuilder()
                .withStatus(Status.ACTIVE)
                .withOfficeBranch(officeBranch)
                .build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        collaboratorRepo.store(collaborator)
        and: 'Owner of office branch authenticated'
        def collaboratorWithRightPerms = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.WRITE, Resource.COLLABORATOR)] as Set
        )
        def token = authUtil.createAndLoginUser(
                collaboratorWithRightPerms.email(),
                "1234"
        )

        when:
        def response = mockMvc
                .perform(get("/api/collaborators/${collaborator.id()}/")
                        .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == [
                    'id'     : collaborator.toResponse().id,
                    'email'  : collaborator.toResponse().email,
                    'name'   : collaborator.toResponse().name,
                    'status' : collaborator.toResponse().status,
                    'created': collaborator.toResponse().created.toString()
            ]
        }
    }
}

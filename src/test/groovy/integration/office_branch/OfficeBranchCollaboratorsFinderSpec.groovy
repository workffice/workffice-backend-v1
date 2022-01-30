package integration.office_branch

import backoffice.domain.collaborator.CollaboratorRepository
import backoffice.domain.office_branch.OfficeBranchId
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class OfficeBranchCollaboratorsFinderSpec extends Specification {
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

    Faker faker = Faker.instance()

    void "it should return unauthorized when token is not specified"() {
        when:
        def response = mockMvc
                .perform(get("/api/office_branches/1/collaborators/"))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return bad request when id specified is not uuid format"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc
                .perform(get("/api/office_branches/-1/collaborators/")
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
                            'message': 'Office branch id is invalid'
                    ]
            ]
        }
    }

    void "it should return not found when office branch does not exist"() {
        given:
        def officeBranchId = new OfficeBranchId()
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc
                .perform(get("/api/office_branches/${officeBranchId}/collaborators/")
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
                            'message': 'Office branch requested does not exist'
                    ]
            ]
        }
    }

    void "it should return forbidden when auth user is not owner of office branch"() {
        given: 'An office branch with no collaborators'
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and: 'An authenticated user that is not the owner of the office branch'
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc
                .perform(get("/api/office_branches/${officeBranch.id()}/collaborators/")
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
                            'message': 'You do not have access to office branch'
                    ]
            ]
        }
    }

    void "it should return forbidden when collaborator does not have read access to collaborators"() {
        given: 'An office branch with no collaborators'
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and: 'An authenticated collaborator without read access to collaborators'
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.READ, Resource.OFFICE)] as Set
        )
        def token = authUtil.createAndLoginUser(collaborator.email(), "1234")

        when:
        def response = mockMvc
                .perform(get("/api/office_branches/${officeBranch.id()}/collaborators/")
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
                            'message': 'You do not have access to office branch'
                    ]
            ]
        }
    }

    void "it should return ok when collaborators are found successfully"() {
        given: 'An office branch with one collaborator'
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        def collaborator = new CollaboratorBuilder()
                .withOfficeBranch(officeBranch)
                .build()
        collaboratorRepo.store(collaborator)
        and: 'Office branch owner authenticated'
        def token = authUtil.createAndLoginUser(officeBranch.owner().email(), "1234")

        when:
        def response = mockMvc
                .perform(get("/api/office_branches/${officeBranch.id()}/collaborators/")
                        .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == [
                    [
                            'id'     : collaborator.toResponse().id,
                            'email'  : collaborator.toResponse().email,
                            'name'   : collaborator.toResponse().name,
                            'status' : collaborator.toResponse().status,
                            'created': collaborator.toResponse().created.toString()
                    ]
            ]
        }
    }

    void "it should return ok when auth user is collaborator with right access"() {
        given: 'An office branch with one collaborator'
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        def collaborator = new CollaboratorBuilder()
                .withOfficeBranch(officeBranch)
                .build()
        collaboratorRepo.store(collaborator)
        and: 'An authenticated collaborator with right permissions'
        def collaboratorWithRightPerms = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.WRITE, Resource.COLLABORATOR)] as Set
        )
        def token = authUtil.createAndLoginUser(collaboratorWithRightPerms.email(), "1234")

        when:
        def response = mockMvc
                .perform(get("/api/office_branches/${officeBranch.id()}/collaborators/")
                        .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data as Set == [
                    [
                            'id'     : collaborator.toResponse().id,
                            'email'  : collaborator.toResponse().email,
                            'name'   : collaborator.toResponse().name,
                            'status' : collaborator.toResponse().status,
                            'created': collaborator.toResponse().created.toString()
                    ],
                    [
                            'id'     : collaboratorWithRightPerms.toResponse().id,
                            'email'  : collaboratorWithRightPerms.toResponse().email,
                            'name'   : collaboratorWithRightPerms.toResponse().name,
                            'status' : collaboratorWithRightPerms.toResponse().status,
                            'created': collaboratorWithRightPerms.toResponse().created.toString()
                    ]
            ] as Set
        }
    }
}

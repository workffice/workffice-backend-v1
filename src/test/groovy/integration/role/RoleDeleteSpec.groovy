package integration.role

import backoffice.domain.office_branch.OfficeBranch
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.domain.role.*
import backoffice.factories.OfficeBranchBuilder
import backoffice.factories.RoleBuilder
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
class RoleDeleteSpec extends Specification {
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    RoleRepository roleRepo
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
        def response = mockMvc.perform(delete("/api/roles/1/"))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return bad request when role id is invalid"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(delete("/api/roles/-1/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INVALID_ROLE_ID',
                            'message': 'The role id specified is not valid',
                    ]
            ]
        }

    }

    void "it should return not found when there is no role with id specified"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def roleId = new RoleId()

        when:
        def response = mockMvc.perform(delete("/api/roles/${roleId}/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'NOT_FOUND',
                            'error'  : 'ROLE_NOT_FOUND',
                            'message': 'There is no role with id specified',
                    ]
            ]
        }
    }

    void "it should return forbidden whe auth user does not have access"() {
        given:
        def officeBranch = createOfficeBranch()
        def role = new RoleBuilder()
                .withOfficeBranch(officeBranch)
                .build()
        roleRepo.store(role)
        and: "An authenticated user that is not the owner of the role"
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(delete("/api/roles/${role.id()}/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'ROLE_FORBIDDEN',
                            'message': 'You don\'t have access to the role requested',
                    ]
            ]
        }
    }

    void "it should return forbidden when collaborator has no role write access"() {
        given:
        def officeBranch = createOfficeBranch()
        def role = new RoleBuilder()
                .withOfficeBranch(officeBranch)
                .build()
        roleRepo.store(role)
        and: "An authenticated collaborator with insufficient permissions"
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.READ, Resource.ROLE)] as Set
        )
        def token = authUtil.createAndLoginUser(collaborator.email(), "1234")

        when:
        def response = mockMvc.perform(delete("/api/roles/${role.id()}/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'ROLE_FORBIDDEN',
                            'message': 'You don\'t have access to the role requested'
                    ]
            ]
        }
    }

    void "it should return ok when role is deleted successfully by the office branch owner"() {
        given:
        def officeBranch = createOfficeBranch()
        def role = new RoleBuilder()
                .withOfficeBranch(officeBranch)
                .build()
        roleRepo.store(role)
        def token = authUtil.createAndLoginUser(officeBranch.owner().email(), "1234")

        when:
        def response = mockMvc.perform(delete("/api/roles/${role.id()}/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        def roleUpdated = roleRepo.findById(role.id()).get()
        !roleUpdated.isActive()
    }

    void "it should return ok when role is deleted successfully by a collaborator with write access to role"() {
        given:
        def officeBranch = createOfficeBranch()
        def role = new RoleBuilder()
                .withOfficeBranch(officeBranch)
                .build()
        roleRepo.store(role)
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.WRITE, Resource.ROLE)] as Set
        )
        def token = authUtil.createAndLoginUser(collaborator.email(), "1234")

        when:
        def response = mockMvc.perform(delete("/api/roles/${role.id()}/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        def roleUpdated = roleRepo.findById(role.id()).get()
        !roleUpdated.isActive()
    }
}

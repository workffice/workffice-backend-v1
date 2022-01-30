package integration.role

import backoffice.domain.office_branch.OfficeBranchId
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.domain.role.Access
import backoffice.domain.role.Permission
import backoffice.domain.role.Resource
import backoffice.domain.role.RoleRepository
import backoffice.factories.OfficeBranchBuilder
import backoffice.factories.OfficeHolderBuilder
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class RolesFinderSpec extends Specification {
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
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper

    def faker = Faker.instance()

    void "it should return unauthorized when token is not provided"() {
        when:
        def response = mockMvc.perform(get("/api/office_branches/1/roles/"))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return bad request when office branch id is not uuid format"() {
        given:
        def authorization = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(get("/api/office_branches/-1/roles/")
                .header('Authorization', "Bearer ${authorization.token}"))
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

    void "it should return not found when there is no office branch with id specified"() {
        given:
        def authorization = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def nonexistentOfficeBranchId = new OfficeBranchId()

        when:
        def response = mockMvc.perform(get("/api/office_branches/${nonexistentOfficeBranchId}/roles/")
                .header('Authorization', "Bearer ${authorization.token}"))
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

    void "it should return forbidden when auth user does not have access to office branch"() {
        given: 'An office branch owned by wick-john@email.com'
        def officeHolder = new OfficeHolderBuilder().withEmail("wick-john@email.com").build()
        def officeBranch = new OfficeBranchBuilder().withOwner(officeHolder).build()
        def role = new RoleBuilder().withOfficeBranch(officeBranch).build()
        officeHolderRepo.store(officeHolder)
        officeBranchRepo.store(officeBranch)
        roleRepo.store(role)
        and: 'An authenticated user with not access to office branch'
        def authorization = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(get("/api/office_branches/${officeBranch.id()}/roles/")
                .header('Authorization', "Bearer ${authorization.token}"))
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

    void "it should return forbidden when auth user is a collaborator of office branch without enough perms"() {
        given: 'An office branch owned by an office holder'
        def officeBranch = new OfficeBranchBuilder().build()
        def role = new RoleBuilder().withOfficeBranch(officeBranch).build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        roleRepo.store(role)
        and: 'An authenticated collaborator without read access to role resource'
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.READ, Resource.OFFICE)] as Set
        )
        def authorization = authUtil.createAndLoginUser(collaborator.email(), "1234")

        when:
        def response = mockMvc.perform(get("/api/office_branches/${officeBranch.id()}/roles/")
                .header('Authorization', "Bearer ${authorization.token}"))
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

    void "it should return ok when roles are retrieved successfully"() {
        given: '3 roles for office branch'
        def officeHolder = new OfficeHolderBuilder().build()
        def officeBranch = new OfficeBranchBuilder().withOwner(officeHolder).build()
        def role1 = new RoleBuilder()
                .withPermissions([Permission.create(Access.READ, Resource.OFFICE)] as Set)
                .withOfficeBranch(officeBranch).build()
        def role2 = new RoleBuilder()
                .withPermissions([Permission.create(Access.WRITE, Resource.OFFICE)] as Set)
                .withOfficeBranch(officeBranch).build()
        def role3 = new RoleBuilder()
                .withPermissions([Permission.create(Access.READ, Resource.ROLE)] as Set)
                .withOfficeBranch(officeBranch).build()
        officeHolderRepo.store(officeHolder)
        officeBranchRepo.store(officeBranch)
        roleRepo.store(role1)
        roleRepo.store(role2)
        roleRepo.store(role3)
        def authorization = authUtil.createAndLoginUser(officeHolder.email(), "1234")

        when:
        def response = mockMvc.perform(get("/api/office_branches/${officeBranch.id()}/roles/")
                .header('Authorization', "Bearer ${authorization.token}"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data as Set == [
                    [
                            'id'         : role1.id().toString(),
                            'name'       : role1.name(),
                            'permissions': [
                                    [
                                            'access'  : 'READ',
                                            'resource': 'OFFICE'
                                    ]
                            ]
                    ],
                    [
                            'id'         : role2.id().toString(),
                            'name'       : role2.name(),
                            'permissions': [
                                    [
                                            'access'  : 'WRITE',
                                            'resource': 'OFFICE'
                                    ]
                            ]
                    ],
                    [
                            'id'         : role3.id().toString(),
                            'name'       : role3.name(),
                            'permissions': [
                                    [
                                            'access'  : 'READ',
                                            'resource': 'ROLE'
                                    ]
                            ]
                    ],
            ] as Set
        }
    }

    void "it should return ok when roles are retrieved successfully for a collaborator with right perms"() {
        given: 'An office branch'
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and: 'An existent role with read access over role resource'
        def role1 = new RoleBuilder()
                .withPermissions([Permission.create(Access.READ, Resource.ROLE)] as Set)
                .withOfficeBranch(officeBranch).build()
        roleRepo.store(role1)
        and: 'An authenticated collaborator with the read access over role resource'
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                role1
        )
        def authorization = authUtil.createAndLoginUser(collaborator.email(), "1234")

        when:
        def response = mockMvc.perform(get("/api/office_branches/${officeBranch.id()}/roles/")
                .header('Authorization', "Bearer ${authorization.token}"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data as Set == [
                    [
                            'id'         : role1.id().toString(),
                            'name'       : role1.name(),
                            'permissions': [
                                    [
                                            'access'  : 'READ',
                                            'resource': 'ROLE'
                                    ]
                            ]
                    ]
            ] as Set
        }
    }
}

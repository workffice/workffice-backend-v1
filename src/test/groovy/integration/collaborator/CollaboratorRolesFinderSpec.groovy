package integration.collaborator

import backoffice.domain.collaborator.CollaboratorId
import backoffice.domain.collaborator.CollaboratorRepository
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.domain.role.Access
import backoffice.domain.role.Permission
import backoffice.domain.role.Resource
import backoffice.domain.role.RoleRepository
import backoffice.factories.CollaboratorBuilder
import backoffice.factories.OfficeBranchBuilder
import backoffice.factories.RoleBuilder
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
class CollaboratorRolesFinderSpec extends Specification {
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    CollaboratorRepository collaboratorRepo
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

    void "it should return not authorized when token is not provided"() {
        when:
        def response = mockMvc
                .perform(get("/api/collaborators/1/roles/"))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return bad request when collaborator id is not uuid format"() {
        given:
        def token = authUtil.createAndLoginUser("apt@mail.com", "1234")

        when:
        def response = mockMvc.perform(get("/api/collaborators/-1/roles/")
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

    void "it should return not found when collaborator does not exist"() {
        given:
        def collaboratorId = new CollaboratorId()
        def token = authUtil.createAndLoginUser("bn@mail.com", "1234")

        when:
        def response = mockMvc.perform(get("/api/collaborators/${collaboratorId}/roles/")
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
        given: 'A collaborator with one role'
        def officeBranch = new OfficeBranchBuilder().build()
        def role = new RoleBuilder().withOfficeBranch(officeBranch).build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        roleRepo.store(role)
        def collaborator = new CollaboratorBuilder()
                .withOfficeBranch(officeBranch)
                .addRole(role)
                .build()
        collaboratorRepo.store(collaborator)
        and: 'An authenticated user that is not the owner of the office branch'
        def token = authUtil.createAndLoginUser("pp@mail.com", "1234")

        when:
        def response = mockMvc.perform(get("/api/collaborators/${collaborator.id()}/roles/")
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

    void "it should return forbidden when collaborator has no read permission to collaborator resource"() {
        given: 'A collaborator with one role'
        def officeBranch = new OfficeBranchBuilder().build()
        def role = new RoleBuilder().withOfficeBranch(officeBranch).build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        roleRepo.store(role)
        def collaborator = new CollaboratorBuilder()
                .withOfficeBranch(officeBranch)
                .addRole(role)
                .build()
        collaboratorRepo.store(collaborator)
        and: 'An authenticated collaborator that does not have read access to collaborators'
        def collaboratorWithoutPerms = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.READ, Resource.OFFICE)] as Set
        )
        def token = authUtil.createAndLoginUser(collaboratorWithoutPerms.email(), "1234")

        when:
        def response = mockMvc.perform(get("/api/collaborators/${collaborator.id()}/roles/")
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

    void "it should return ok when roles are found successfully"() {
        given: 'A collaborator with 2 role'
        def officeBranch = new OfficeBranchBuilder().build()
        def role = new RoleBuilder()
                .withPermissions([Permission.create(Access.WRITE, Resource.OFFICE)] as Set)
                .withOfficeBranch(officeBranch)
                .build()
        def role2 = new RoleBuilder()
                .withOfficeBranch(officeBranch)
                .withPermissions([
                        Permission.create(Access.WRITE, Resource.OFFICE),
                        Permission.create(Access.READ, Resource.ROLE)
                ] as Set)
                .build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        roleRepo.store(role)
        roleRepo.store(role2)
        def collaborator = new CollaboratorBuilder()
                .withOfficeBranch(officeBranch)
                .addRole(role).addRole(role2)
                .build()
        collaboratorRepo.store(collaborator)
        and: 'An authenticated user that is the owner of the office branch'
        def token = authUtil.createAndLoginUser(officeBranch.owner().email(), "1234")

        when:
        def response = mockMvc.perform(get("/api/collaborators/${collaborator.id()}/roles/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data as Set == [
                    [
                            'id'       : role.toResponse().id,
                            'name'     : role.toResponse().name,
                            permissions: [
                                    [
                                            'resource': role.toResponse().permissions[0].resource,
                                            'access'  : role.toResponse().permissions[0].access
                                    ],
                            ]
                    ],
                    [
                            'id'       : role2.toResponse().id,
                            'name'     : role2.toResponse().name,
                            permissions: [
                                    [
                                            'resource': role2.toResponse().permissions[0].resource,
                                            'access'  : role2.toResponse().permissions[0].access
                                    ],
                                    [
                                            'resource': role2.toResponse().permissions[1].resource,
                                            'access'  : role2.toResponse().permissions[1].access
                                    ],
                            ]
                    ]
            ] as Set
        }
    }

    void "it should return ok when auth user is collaborator with right permissions"() {
        given: 'A collaborator with one role'
        def officeBranch = new OfficeBranchBuilder().build()
        def role = new RoleBuilder()
                .withOfficeBranch(officeBranch)
                .withPermissions([Permission.create(Access.WRITE, Resource.ROLE)] as Set)
                .build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        roleRepo.store(role)
        def collaborator = new CollaboratorBuilder()
                .withOfficeBranch(officeBranch)
                .addRole(role)
                .build()
        collaboratorRepo.store(collaborator)
        and: 'An authenticated collaborator that have read access to collaborators'
        def collaboratorWithoutPerms = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.READ, Resource.COLLABORATOR)] as Set
        )
        def token = authUtil.createAndLoginUser(collaboratorWithoutPerms.email(), "1234")

        when:
        def response = mockMvc.perform(get("/api/collaborators/${collaborator.id()}/roles/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data as Set == [
                    [
                            'id'       : role.toResponse().id,
                            'name'     : role.toResponse().name,
                            permissions: [
                                    [
                                            'resource': role.toResponse().permissions[0].resource,
                                            'access'  : role.toResponse().permissions[0].access
                                    ],
                            ]
                    ]
            ] as Set
        }
    }
}

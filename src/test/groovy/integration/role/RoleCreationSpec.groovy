package integration.role

import backoffice.application.dto.role.RoleInformation
import backoffice.domain.office_branch.OfficeBranchId
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.domain.role.Access
import backoffice.domain.role.Permission
import backoffice.domain.role.Resource
import backoffice.factories.OfficeBranchBuilder
import backoffice.factories.OfficeHolderBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.Sets
import controller.AuthUtil
import controller.CollaboratorUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import server.WorkfficeApplication
import spock.lang.Shared
import spock.lang.Specification

import static controller.IntegrationTestUtil.asJsonString
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class RoleCreationSpec extends Specification {
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    CollaboratorUtil collaboratorUtil
    @Autowired
    AuthUtil authUtil
    @Autowired
    ObjectMapper objectMapper
    @Autowired
    MockMvc mockMvc

    @Shared
            missingName = [
                    'code'   : 'INVALID',
                    'error'  : 'INVALID_NAME',
                    'message': 'Role name is required'
            ]
    @Shared
            missingPermissions = [
                    'code'   : 'INVALID',
                    'error'  : 'INVALID_PERMISSIONS',
                    'message': 'Role Permissions are required'
            ]

    private static MockHttpServletRequestBuilder roleCreationRequest(
            String officeBranchId,
            RoleInformation roleInformation,
            String token
    ) {
        return post("/api/office_branches/${officeBranchId}/roles/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(roleInformation))
                .header("Authorization", "Bearer ${token}")
    }

    void "it should return unauthorized when token is not provided"() {
        given:
        def roleInformation = RoleInformation.of(
                "New awesome role",
                [new RoleInformation.Permission("ROLE", "READ")]
        )

        when:
        def response = mockMvc
                .perform(post("/api/office_branches/1/roles/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(roleInformation)))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return bad request when office branch id is invalid"() {
        given:
        def roleInformation = RoleInformation.of(
                "New awesome role",
                [new RoleInformation.Permission("ROLE", "READ")]
        )
        def authentication = authUtil.createAndLoginUser("john@wick.com", "1234")

        when:
        def response = mockMvc
                .perform(roleCreationRequest('-1', roleInformation, authentication.token))
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
        def roleInformation = RoleInformation.of(
                "New awesome role",
                [new RoleInformation.Permission("ROLE", "READ")]
        )
        def authentication = authUtil.createAndLoginUser("john@wick.com", "1234")
        def officeBranchId = new OfficeBranchId()

        when:
        def response = mockMvc
                .perform(roleCreationRequest(officeBranchId.toString(), roleInformation, authentication.token))
                .andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()
    }

    void "it should return forbidden when auth user does not have access to office branch"() {
        given: 'An authenticated user with email bla@bla.com'
        def authentication = authUtil.createAndLoginUser("bla@bla.com", "12")
        and: 'An office holder with email anotherUser@email.com'
        def officeHolder = new OfficeHolderBuilder().withEmail("anotherUser@email.com").build()
        def officeBranch = new OfficeBranchBuilder().withOwner(officeHolder).build()
        officeHolderRepo.store(officeHolder)
        officeBranchRepo.store(officeBranch)
        def roleInformation = RoleInformation.of(
                "New awesome role",
                [new RoleInformation.Permission("ROLE", "READ")]
        )

        when:
        def response = mockMvc
                .perform(roleCreationRequest(officeBranch.id().toString(), roleInformation, authentication.token))
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

    void "it should return forbidden when auth user is a collaborator with no perms"() {
        given: 'A office branch owned by an office holder'
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and: 'An authenticated collaborator with insufficient perms'
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.READ, Resource.ROLE)] as Set
        )
        def roleInformation = RoleInformation.of(
                "New awesome role",
                [new RoleInformation.Permission("ROLE", "READ")]
        )
        def authentication = authUtil.createAndLoginUser(collaborator.email(), "12")

        when:
        def response = mockMvc
                .perform(roleCreationRequest(officeBranch.id().toString(), roleInformation, authentication.token))
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

    void "it should return created when role is created successfully"() {
        given: 'An authenticated user with email anotherUser@email.com'
        def officeHolder = new OfficeHolderBuilder().build()
        def officeBranch = new OfficeBranchBuilder().withOwner(officeHolder).build()
        officeHolderRepo.store(officeHolder)
        officeBranchRepo.store(officeBranch)
        def authentication = authUtil.createAndLoginUser(officeHolder.email(), "12")
        def roleInformation = RoleInformation.of(
                "New awesome role",
                [new RoleInformation.Permission("ROLE", "READ")]
        )

        when:
        def response = mockMvc
                .perform(roleCreationRequest(officeBranch.id().toString(), roleInformation, authentication.token))
                .andReturn().response

        then:
        response.status == HttpStatus.CREATED.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data['uri'] ==~ "/api/roles/.*/"
        }
    }

    void "it should return created when role is created successfully by a collaborator with perms"() {
        given: 'An office branch owned by an office holder'
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and: 'An authenticated collaborator with right perms'
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.WRITE, Resource.ROLE)] as Set
        )
        def roleInformation = RoleInformation.of(
                "New awesome role",
                [new RoleInformation.Permission("ROLE", "READ")]
        )
        def authentication = authUtil.createAndLoginUser(collaborator.email(), "12")

        when:
        def response = mockMvc
                .perform(roleCreationRequest(officeBranch.id().toString(), roleInformation, authentication.token))
                .andReturn().response

        then:
        response.status == HttpStatus.CREATED.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data['uri'] ==~ "/api/roles/.*/"
        }
    }

    void "it should return bad request when there are errors in the fields"() {
        given: 'An authenticated user'
        def officeHolder = new OfficeHolderBuilder().build()
        def officeBranch = new OfficeBranchBuilder().withOwner(officeHolder).build()
        officeHolderRepo.store(officeHolder)
        officeBranchRepo.store(officeBranch)
        def authentication = authUtil.createAndLoginUser(officeHolder.email(), "12")
        and: 'A role information invalid'
        def roleInformation = RoleInformation.of(name, permissions)

        when:
        def response = mockMvc
                .perform(roleCreationRequest(officeBranch.id().toString(), roleInformation, authentication.token))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            Sets.newHashSet(it.errors) == Sets.newHashSet(expectedErrors)
        }

        where:
        name        | permissions                                         || expectedErrors
        ""          | [new RoleInformation.Permission("ROLE", "READ")]    || [missingName]
        null        | [new RoleInformation.Permission("ROLE", "READ")]    || [missingName]
        "some name" | [new RoleInformation.Permission("", "")]            || [missingPermissionResource(0), missingPermissionAccess(0)]
        "some name" | [new RoleInformation.Permission(null, null)]        || [missingPermissionResource(0), missingPermissionAccess(0)]
        "some name" | [new RoleInformation.Permission("INVALID", "READ"),
                       new RoleInformation.Permission("ROLE", "INVALID")] || [invalidPermissionResource(0), invalidPermissionAccess(1)]
        "some"      | []                                                  || [missingPermissions]
    }

    static Map invalidPermissionResource(int index) {
        [
                'code'   : 'INVALID',
                'error'  : "INVALID_PERMISSIONS[${index}].RESOURCE",
                'message': 'Permission Resource is invalid'
        ]
    }

    static Map invalidPermissionAccess(int index) {
        [
                'code'   : 'INVALID',
                'error'  : "INVALID_PERMISSIONS[${index}].ACCESS",
                'message': 'Permission Access is invalid'
        ]
    }

    static Map missingPermissionResource(int index) {
        [
                'code'   : 'INVALID',
                'error'  : "INVALID_PERMISSIONS[${index}].RESOURCE",
                'message': 'Permission Resource is required'
        ]
    }

    static Map missingPermissionAccess(int index) {
        [
                'code'   : 'INVALID',
                'error'  : "INVALID_PERMISSIONS[${index}].ACCESS",
                'message': 'Permission Access is required'
        ]
    }
}

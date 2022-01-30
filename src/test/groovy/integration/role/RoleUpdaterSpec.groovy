package integration.role

import backoffice.application.dto.role.RoleInformation
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.domain.role.*
import backoffice.factories.OfficeBranchBuilder
import backoffice.factories.OfficeHolderBuilder
import backoffice.factories.RoleBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.Sets
import controller.AuthUtil
import controller.CollaboratorUtil
import controller.IntegrationTestUtil
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class RoleUpdaterSpec extends Specification {
    @Autowired
    RoleRepository roleRepo
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    AuthUtil authUtil
    @Autowired
    CollaboratorUtil collaboratorUtil
    @Autowired
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper

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

    private static MockHttpServletRequestBuilder updateRoleRequest(
            String roleId,
            RoleInformation roleInformation,
            String token
    ) {
        put("/api/roles/${roleId}/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(IntegrationTestUtil.asJsonString(roleInformation))
                .header('Authorization', "Bearer ${token}")
    }

    void "it should return unauthorized when token is not provided"() {
        given:
        def roleInformation = RoleInformation.of(
                "Some role",
                [new RoleInformation.Permission("OFFICE_BRANCH", "READ")]
        )

        when:
        def response = mockMvc.perform(put("/api/roles/1/")
                .contentType(MediaType.APPLICATION_JSON)
                .contentType(IntegrationTestUtil.asJsonString(roleInformation)))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return bad request when role id is not uuid format"() {
        given:
        def roleInformation = RoleInformation.of(
                "Some role",
                [new RoleInformation.Permission("OFFICE", "READ")]
        )
        def authorization = authUtil.createAndLoginUser("some@email.com", "pa55word")

        when:
        def response = mockMvc
                .perform(updateRoleRequest("-1", roleInformation, authorization.token))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INVALID_ROLE_ID',
                            'message': 'The role id specified is not valid'
                    ]
            ]
        }
    }

    void "it should return not found when there is no role with id specified"() {
        given:
        def roleInformation = RoleInformation.of(
                "Some role",
                [new RoleInformation.Permission("OFFICE", "READ")]
        )
        def authorization = authUtil.createAndLoginUser("some2@email.com", "pa55word")

        when:
        def response = mockMvc
                .perform(updateRoleRequest(new RoleId().toString(), roleInformation, authorization.token))
                .andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'NOT_FOUND',
                            'error'  : 'ROLE_NOT_FOUND',
                            'message': 'There is no role with id specified'
                    ]
            ]
        }
    }

    void "it should return forbidden when auth user does not have access to role"() {
        given: 'A role owned by wick@john.com'
        def officeHolder = new OfficeHolderBuilder().withEmail('wick@john.com').build()
        def officeBranch = new OfficeBranchBuilder().withOwner(officeHolder).build()
        def role = new RoleBuilder().withOfficeBranch(officeBranch).build()
        officeHolderRepo.store(officeHolder)
        officeBranchRepo.store(officeBranch)
        roleRepo.store(role)
        and: 'An authenticated user with email scaloneta@mail.com'
        def authentication = authUtil.createAndLoginUser('scaloneta@mail.com', 'pa55')
        def roleInformation = RoleInformation.of(
                "Some role",
                [new RoleInformation.Permission("OFFICE", "READ")]
        )

        when:
        def response = mockMvc
                .perform(updateRoleRequest(role.id().toString(), roleInformation, authentication.token))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'ROLE_FORBIDDEN',
                            'message': "You don't have access to the role requested"
                    ]
            ]
        }
    }

    void "it should return forbidden when collaborator does not have write access for role resource"() {
        given: 'A role owned by an office holder'
        def officeHolder = new OfficeHolderBuilder().build()
        def officeBranch = new OfficeBranchBuilder().withOwner(officeHolder).build()
        def role = new RoleBuilder().withOfficeBranch(officeBranch).build()
        officeHolderRepo.store(officeHolder)
        officeBranchRepo.store(officeBranch)
        roleRepo.store(role)
        and: 'An authenticated collaborator without write access to role resource'
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.READ, Resource.ROLE)] as Set
        )
        def authentication = authUtil.createAndLoginUser(collaborator.email(), 'pa55')
        def roleInformation = RoleInformation.of(
                "Some role",
                [new RoleInformation.Permission("OFFICE", "READ")]
        )

        when:
        def response = mockMvc
                .perform(updateRoleRequest(role.id().toString(), roleInformation, authentication.token))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'ROLE_FORBIDDEN',
                            'message': "You don't have access to the role requested"
                    ]
            ]
        }
    }

    void "it should return ok with role properties updated when update is successful"() {
        given:
        def officeHolder = new OfficeHolderBuilder().build()
        def officeBranch = new OfficeBranchBuilder().withOwner(officeHolder).build()
        def role = new RoleBuilder()
                .withName("Old name")
                .withPermissions([Permission.create(Access.READ, Resource.ROLE)] as Set<Permission>)
                .withOfficeBranch(officeBranch)
                .build()
        officeHolderRepo.store(officeHolder)
        officeBranchRepo.store(officeBranch)
        roleRepo.store(role)
        def authentication = authUtil.createAndLoginUser(officeHolder.email(), 'pa55')
        def roleInformation = RoleInformation.of(
                "New awesome name",
                [
                        new RoleInformation.Permission("OFFICE", "READ"),
                        new RoleInformation.Permission("ROLE", "READ"),
                ]
        )

        when:
        def response = mockMvc
                .perform(updateRoleRequest(role.id().toString(), roleInformation, authentication.token))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data['id'] == role.id().toString()
            it.data['name'] == 'New awesome name'
            Sets.newHashSet(it.data['permissions']) == Sets.newHashSet([
                    [
                            'resource': 'ROLE',
                            'access'  : 'READ'
                    ],
                    [
                            'resource': 'OFFICE',
                            'access'  : 'READ'
                    ],
            ])
        }
        and:
        def roleUpdated = roleRepo.findById(role.id()).get()
        roleUpdated.name() == 'New awesome name'
        roleUpdated.permissions() == [
                Permission.create(Access.READ, Resource.OFFICE),
                Permission.create(Access.READ, Resource.ROLE)
        ] as Set<Permission>
    }

    void "it should return ok with role properties updated with a collaborator with write access to roles"() {
        given: 'A role owned by an office holder'
        def officeHolder = new OfficeHolderBuilder().build()
        def officeBranch = new OfficeBranchBuilder().withOwner(officeHolder).build()
        def role = new RoleBuilder().withOfficeBranch(officeBranch).build()
        officeHolderRepo.store(officeHolder)
        officeBranchRepo.store(officeBranch)
        roleRepo.store(role)
        and: 'An authenticated collaborator with write access to role resource'
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.WRITE, Resource.ROLE)] as Set
        )
        def authentication = authUtil.createAndLoginUser(collaborator.email(), 'pa55')
        def roleInformation = RoleInformation.of(
                "New awesome name",
                [
                        new RoleInformation.Permission("OFFICE", "READ"),
                        new RoleInformation.Permission("ROLE", "READ"),
                ]
        )

        when:
        def response = mockMvc
                .perform(updateRoleRequest(role.id().toString(), roleInformation, authentication.token))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data['id'] == role.id().toString()
            it.data['name'] == 'New awesome name'
            Sets.newHashSet(it.data['permissions']) == Sets.newHashSet([
                    [
                            'resource': 'ROLE',
                            'access'  : 'READ'
                    ],
                    [
                            'resource': 'OFFICE',
                            'access'  : 'READ'
                    ],
            ])
        }
        and:
        def roleUpdated = roleRepo.findById(role.id()).get()
        roleUpdated.name() == 'New awesome name'
        roleUpdated.permissions() == [
                Permission.create(Access.READ, Resource.OFFICE),
                Permission.create(Access.READ, Resource.ROLE)
        ] as Set<Permission>
    }

    void "it should return bad request when input fields are incorrect"() {
        given:
        def officeHolder = new OfficeHolderBuilder().build()
        def officeBranch = new OfficeBranchBuilder().withOwner(officeHolder).build()
        def role = new RoleBuilder()
                .withName("Old name")
                .withPermissions([Permission.create(Access.READ, Resource.ROLE)] as Set<Permission>)
                .withOfficeBranch(officeBranch)
                .build()
        officeHolderRepo.store(officeHolder)
        officeBranchRepo.store(officeBranch)
        roleRepo.store(role)
        def authentication = authUtil.createAndLoginUser(officeHolder.email(), 'pa55')
        def roleInformation = RoleInformation.of(name, permissions)

        when:
        def response = mockMvc
                .perform(updateRoleRequest(role.id().toString(), roleInformation, authentication.token))
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

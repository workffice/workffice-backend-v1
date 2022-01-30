package integration.collaborator

import backoffice.application.dto.collaborator.CollaboratorInformation
import backoffice.domain.collaborator.CollaboratorRepository
import backoffice.domain.office_branch.OfficeBranchId
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
import com.github.javafaker.Faker
import com.google.common.collect.ImmutableSet
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
class CollaboratorCreationSpec extends Specification {
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    RoleRepository roleRepository
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

    @Shared
    def missingEmailAddress = ['code': 'INVALID', 'error': 'INVALID_EMAIL', 'message': 'Email is required']
    @Shared
    def missingName = ['code': 'INVALID', 'error': 'INVALID_NAME', 'message': 'Name is required']
    @Shared
    def invalidEmailAddress = ['code': 'INVALID', 'error': 'INVALID_EMAIL', 'message': 'Email format is invalid']
    @Shared
    def missingRoleIds = ['code': 'INVALID', 'error': 'INVALID_ROLEIDS', 'message': 'Role ids are required']
    @Shared
    def invalidRoleIds = ['code': 'INVALID', 'error': 'INVALID_ROLEIDS', 'message': 'Input type is invalid']

    private static MockHttpServletRequestBuilder collaboratorCreationRequest(
            CollaboratorInformation collaboratorInfo,
            String officeBranchId,
            String token
    ) {
        post("/api/office_branches/${officeBranchId}/collaborators/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(collaboratorInfo))
                .header("Authorization", "Bearer ${token}")
    }

    void "it should return unauthorized when token is not provided"() {
        given:
        def collaboratorInfo = CollaboratorInformation.of(
                ImmutableSet.of(UUID.randomUUID()),
                "some@email.com",
                "pirlonzio"
        )

        when:
        def response = mockMvc
                .perform(post("/api/office_branches/1/collaborators/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(collaboratorInfo)))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return bad request when office branch id is not uuid format"() {
        given:
        def collaboratorInfo = CollaboratorInformation.of(
                ImmutableSet.of(UUID.randomUUID()),
                "some@email.com",
                "pirlonzio"
        )
        def authorization = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc
                .perform(collaboratorCreationRequest(collaboratorInfo, "-1", authorization.token))
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

    void "it should return not found when there is no office branch with id provided"() {
        given:
        def collaboratorInfo = CollaboratorInformation.of(
                ImmutableSet.of(UUID.randomUUID()),
                "some@email.com",
                "munieco"
        )
        def authorization = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        String officeBranchId = new OfficeBranchId().toString()

        when:
        def response = mockMvc
                .perform(collaboratorCreationRequest(collaboratorInfo, officeBranchId, authorization.token))
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
        given: 'An office holder with an office branch'
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        String officeBranchId = officeBranch.id().toString()
        def collaboratorInfo = CollaboratorInformation.of(
                ImmutableSet.of(UUID.randomUUID()),
                "some@email.com",
                "munieco"
        )
        and: 'An authenticated user with different email address'
        def authorization = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc
                .perform(collaboratorCreationRequest(collaboratorInfo, officeBranchId, authorization.token))
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

    void "it should return forbidden when collaborator does not have write access to collaborator resource for an office branch"() {
        given: 'An office holder with an office branch'
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        String officeBranchId = officeBranch.id().toString()
        and: 'An authenticated collaborator without write access to collaborator resource'
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.READ, Resource.COLLABORATOR), Permission.create(Access.WRITE, Resource.ROLE)] as Set
        )
        def authorization = authUtil.createAndLoginUser(collaborator.email(), "1234")
        def collaboratorInfo = CollaboratorInformation.of(
                ImmutableSet.of(UUID.randomUUID()),
                "some@email.com",
                "munieco"
        )

        when:
        def response = mockMvc
                .perform(collaboratorCreationRequest(collaboratorInfo, officeBranchId, authorization.token))
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

    void "it should return bad request when collaborator email already exists for the same office branch"() {
        given: 'An existent office branch'
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        String officeBranchId = officeBranch.id().toString()
        and: 'An existent collaborator'
        def collaborator = new CollaboratorBuilder().withOfficeBranch(officeBranch).build()
        collaboratorRepo.store(collaborator)
        and: 'A collaborator info with email that already exist'
        def collaboratorInfo = CollaboratorInformation.of(
                ImmutableSet.of(UUID.randomUUID()),
                collaborator.email(),
                "munieco"
        )
        def authorization = authUtil.createAndLoginUser(officeBranch.owner().email(), "1234")

        when:
        def response = mockMvc
                .perform(collaboratorCreationRequest(collaboratorInfo, officeBranchId, authorization.token))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'COLLABORATOR_ALREADY_EXISTS',
                            'message': 'There is a collaborator with the same email for the office branch requested'
                    ]
            ]
        }
    }

    void "it should return created when collaborator is created successfully"() {
        given: 'An existent collaborator'
        def officeBranch = new OfficeBranchBuilder().build()
        String officeBranchId = officeBranch.id().toString()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and: 'An existent role'
        def role = new RoleBuilder().withOfficeBranch(officeBranch).build()
        roleRepository.store(role)
        and: "A brand new collaborator 🖖"
        def collaboratorInfo = CollaboratorInformation.of(
                ImmutableSet.of(UUID.fromString(role.id().toString())),
                "example@email.com",
                "munieco"
        )
        def authorization = authUtil.createAndLoginUser(officeBranch.owner().email(), "1234")

        when:
        def response = mockMvc
                .perform(collaboratorCreationRequest(collaboratorInfo, officeBranchId, authorization.token))
                .andReturn().response

        then:
        response.status == HttpStatus.CREATED.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data['uri'] ==~ "/api/collaborators/.*/"
        }
        and:
        collaboratorRepo.exists("example@email.com", officeBranch)
    }

    void "it should return created when collaborator with right perms creates another collaborator"() {
        given: 'An existent collaborator'
        def officeBranch = new OfficeBranchBuilder().build()
        String officeBranchId = officeBranch.id().toString()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and: 'An existent role'
        def role = new RoleBuilder().withOfficeBranch(officeBranch).build()
        roleRepository.store(role)
        and: "An authenticated collaborator with write access to collaborator resource"
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.WRITE, Resource.COLLABORATOR), Permission.create(Access.WRITE, Resource.ROLE)] as Set
        )
        def authorization = authUtil.createAndLoginUser(collaborator.email(), "1234")
        def collaboratorInfo = CollaboratorInformation.of(
                ImmutableSet.of(UUID.fromString(role.id().toString())),
                "example@email.com",
                "munieco"
        )

        when:
        def response = mockMvc
                .perform(collaboratorCreationRequest(collaboratorInfo, officeBranchId, authorization.token))
                .andReturn().response

        then:
        response.status == HttpStatus.CREATED.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data['uri'] ==~ "/api/collaborators/.*/"
        }
        and:
        collaboratorRepo.exists("example@email.com", officeBranch)
    }

    void "it should return bad request when collaborator information is invalid"() {
        given: 'An existent collaborator'
        def officeBranch = new OfficeBranchBuilder().build()
        String officeBranchId = officeBranch.id().toString()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and: 'An invalid collaborator info'
        def collaboratorInfo = [
                "roleIds": roleIds,
                "email"  : collaboratorEmail,
                "name"   : name
        ]
        def authorization = authUtil.createAndLoginUser(officeBranch.owner().email(), "1234")

        when:
        def response = mockMvc
                .perform(post("/api/office_branches/${officeBranchId}/collaborators/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(collaboratorInfo))
                        .header("Authorization", "Bearer ${authorization.token}"))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors as Set == errorsExpected
        }

        where:
        roleIds   | collaboratorEmail  | name        || errorsExpected
        [] as Set | "scalonetabla.com" | "pirlonzio" || [missingRoleIds, invalidEmailAddress] as Set
        null      | ""                 | "pirlonzio" || [missingRoleIds, missingEmailAddress] as Set
        null      | ""                 | ""          || [missingRoleIds, missingEmailAddress, missingName] as Set
        ["1"]     | null               | "napoleon"  || [invalidRoleIds] as Set // When type is invalid only the first error is took
    }
}

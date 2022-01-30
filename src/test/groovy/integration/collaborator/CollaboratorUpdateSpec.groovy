package integration.collaborator

import authentication.application.dto.token.Authentication
import backoffice.application.dto.collaborator.CollaboratorUpdateInformation
import backoffice.domain.collaborator.CollaboratorId
import backoffice.domain.collaborator.CollaboratorRepository
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.domain.role.Access
import backoffice.domain.role.Permission
import backoffice.domain.role.Resource
import backoffice.domain.role.RoleRepository
import backoffice.factories.CollaboratorBuilder
import backoffice.factories.RoleBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.javafaker.Faker
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class CollaboratorUpdateSpec extends Specification {
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

    Faker faker = Faker.instance()

    @Shared
    def missingName = ['code': 'INVALID', 'error': 'INVALID_NAME', 'message': 'Name is required']
    @Shared
    def missingRoleIds = ['code': 'INVALID', 'error': 'INVALID_ROLEIDS', 'message': 'Role ids are required']

    private static MockHttpServletRequestBuilder updateCollaboratorRequest(
            String collaboratorId,
            CollaboratorUpdateInformation collaboratorUpdateInfo,
            Authentication token
    ) {
        put("/api/collaborators/${collaboratorId}/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(collaboratorUpdateInfo))
                .header("Authorization", "Bearer ${token.getToken()}")
    }

    void "it should return not authorized when token is not provided"() {
        given:
        def collaboratorUpdateInfo = CollaboratorUpdateInformation.of(
                "new name",
                [UUID.randomUUID()] as Set
        )

        when:
        def response = mockMvc.perform(put("/api/collaborators/1/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(collaboratorUpdateInfo)))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return bad request when collaborator id is not uuid format"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def collaboratorUpdateInfo = CollaboratorUpdateInformation.of(
                "new name",
                [UUID.randomUUID()] as Set
        )

        when:
        def response = mockMvc.perform(updateCollaboratorRequest("-1", collaboratorUpdateInfo, token))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
    }

    void "it should return not found when there is no collaborator with id provided"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def collaboratorId = new CollaboratorId()
        def collaboratorUpdateInfo = CollaboratorUpdateInformation.of(
                "new name",
                [UUID.randomUUID()] as Set
        )

        when:
        def response = mockMvc.perform(updateCollaboratorRequest(collaboratorId.toString(), collaboratorUpdateInfo, token))
                .andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()
    }

    void "it should return forbidden when auth user is not owner of office branch that contains the collaborator"() {
        given:
        def collaborator = new CollaboratorBuilder().build()
        officeHolderRepo.store(collaborator.officeBranch().owner())
        officeBranchRepo.store(collaborator.officeBranch())
        collaboratorRepo.store(collaborator)
        and:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def collaboratorUpdateInfo = CollaboratorUpdateInformation.of(
                "new name",
                [UUID.randomUUID()] as Set
        )

        when:
        def response = mockMvc.perform(updateCollaboratorRequest(collaborator.id().toString(), collaboratorUpdateInfo, token))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
    }

    void "it should return forbidden when auth user is collaborator without write access to collaborator resource"() {
        given:
        def collaborator = new CollaboratorBuilder().build()
        officeHolderRepo.store(collaborator.officeBranch().owner())
        officeBranchRepo.store(collaborator.officeBranch())
        collaboratorRepo.store(collaborator)
        and:
        def officeBranchCollaborator = collaboratorUtil.createCollaborator(
                collaborator.officeBranch(),
                [Permission.create(Access.READ, Resource.COLLABORATOR)] as Set
        )
        def token = authUtil.createAndLoginUser(officeBranchCollaborator.email(), "1234")
        def collaboratorUpdateInfo = CollaboratorUpdateInformation.of(
                "new name",
                [UUID.randomUUID()] as Set
        )

        when:
        def response = mockMvc.perform(updateCollaboratorRequest(collaborator.id().toString(), collaboratorUpdateInfo, token))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
    }

    void "it should return ok collaborator is updated successfully by office branch owner"() {
        given:
        def collaborator = new CollaboratorBuilder().build()
        officeHolderRepo.store(collaborator.officeBranch().owner())
        officeBranchRepo.store(collaborator.officeBranch())
        collaboratorRepo.store(collaborator)
        def role = new RoleBuilder()
                .withOfficeBranch(collaborator.officeBranch())
                .build()
        roleRepo.store(role)
        and:
        def token = authUtil.createAndLoginUser(collaborator.officeBranch().owner().email(), "1234")
        def collaboratorUpdateInfo = CollaboratorUpdateInformation.of(
                "new name",
                [UUID.fromString(role.id().toString())] as Set
        )

        when:
        def response = mockMvc.perform(updateCollaboratorRequest(collaborator.id().toString(), collaboratorUpdateInfo, token))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        def collaboratorUpdated = collaboratorRepo.findWithRoles(collaborator.id()).get()
        collaboratorUpdated.name() == "new name"
        collaboratorUpdated.roles() == [role] as Set
    }

    void "it should return ok collaborator is updated successfully by another collaborator with right access"() {
        given:
        def collaborator = new CollaboratorBuilder().build()
        officeHolderRepo.store(collaborator.officeBranch().owner())
        officeBranchRepo.store(collaborator.officeBranch())
        collaboratorRepo.store(collaborator)
        def role = new RoleBuilder()
                .withOfficeBranch(collaborator.officeBranch())
                .build()
        roleRepo.store(role)
        and:
        def officeBranchCollaborator = collaboratorUtil.createCollaborator(
                collaborator.officeBranch(),
                [Permission.create(Access.WRITE, Resource.COLLABORATOR)] as Set
        )
        def token = authUtil.createAndLoginUser(officeBranchCollaborator.email(), "1234")
        def collaboratorUpdateInfo = CollaboratorUpdateInformation.of(
                "new name",
                [UUID.fromString(role.id().toString())] as Set
        )

        when:
        def response = mockMvc.perform(updateCollaboratorRequest(collaborator.id().toString(), collaboratorUpdateInfo, token))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        def collaboratorUpdated = collaboratorRepo.findWithRoles(collaborator.id()).get()
        collaboratorUpdated.name() == "new name"
        collaboratorUpdated.roles() == [role] as Set
    }

    void "it should return bad request when update information is invalid"() {
        given:
        def collaborator = new CollaboratorBuilder().build()
        officeHolderRepo.store(collaborator.officeBranch().owner())
        officeBranchRepo.store(collaborator.officeBranch())
        collaboratorRepo.store(collaborator)
        and:
        def token = authUtil.createAndLoginUser(collaborator.officeBranch().owner().email(), "1234")
        def collaboratorUpdateInfo = CollaboratorUpdateInformation.of(
                name,
                roleIds as Set
        )

        when:
        def response = mockMvc.perform(updateCollaboratorRequest(collaborator.id().toString(), collaboratorUpdateInfo, token))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors as Set == expectedErrors as Set
        }

        where:
        name | roleIds || expectedErrors
        null | []      || [missingName, missingRoleIds]
        ''   | null    || [missingName, missingRoleIds]
    }
}

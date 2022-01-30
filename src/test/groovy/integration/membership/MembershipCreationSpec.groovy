package integration.membership

import backoffice.application.dto.membership.MembershipInformation
import backoffice.domain.office_branch.OfficeBranchId
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.domain.role.Access
import backoffice.domain.role.Permission
import backoffice.domain.role.Resource
import backoffice.factories.OfficeBranchBuilder
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
import server.WorkfficeApplication
import spock.lang.Specification

import static controller.IntegrationTestUtil.asJsonString
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class MembershipCreationSpec extends Specification {
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    AuthUtil authUtil
    @Autowired
    CollaboratorUtil collaboratorUtil
    @Autowired
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper

    Faker faker = Faker.instance()

    void "it should return not authorized when token is not provided"() {
        given:
        def info = MembershipInformation.of("bla", "bla", 10, ["SATURDAY"] as Set)

        when:
        def response = mockMvc.perform(post("/api/office_branches/1/memberships/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info)))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return bad request when office branch id is not uuid format"() {
        given:
        def info = MembershipInformation.of("bla", "bla", 10, ["SATURDAY"] as Set)
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(post("/api/office_branches/1/memberships/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info))
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
                            'message': 'The office branch id provided is invalid',
                    ]
            ]
        }
    }

    void "it should return not found when office branch does not exist"() {
        given:
        def officeBranchId = new OfficeBranchId()
        def info = MembershipInformation.of("bla", "bla", 10, ["SATURDAY"] as Set)
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(post("/api/office_branches/${officeBranchId}/memberships/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info))
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
                            'message': 'The office branch requested does not exist',
                    ]
            ]
        }
    }

    void "it should return forbidden when auth user is not owner of office branch"() {
        given:
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and:
        def info = MembershipInformation.of("bla", "bla", 10, ["SATURDAY"] as Set)
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(post("/api/office_branches/${officeBranch.id()}/memberships/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info))
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'MEMBERSHIP_FORBIDDEN',
                            'message': 'You don\'t have access to the office branch memberships',
                    ]
            ]
        }
    }

    void "it should return forbidden when collaborator has no write perms to membership"() {
        given:
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and:
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.WRITE, Resource.NEWS)] as Set
        )
        def info = MembershipInformation.of("bla", "bla", 10, ["SATURDAY"] as Set)
        def token = authUtil.createAndLoginUser(
                collaborator.email(),
                "1234"
        )

        when:
        def response = mockMvc.perform(post("/api/office_branches/${officeBranch.id()}/memberships/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info))
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'MEMBERSHIP_FORBIDDEN',
                            'message': 'You don\'t have access to the office branch memberships',
                    ]
            ]
        }
    }

    void "it should return accepted when membership is created successfully"() {
        given:
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and:
        def info = MembershipInformation.of("bla", "bla", 10, ["SATURDAY"] as Set)
        def token = authUtil.createAndLoginUser(
                officeBranch.owner().email(),
                "1234"
        )

        when:
        def response = mockMvc.perform(post("/api/office_branches/${officeBranch.id()}/memberships/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info))
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.CREATED.value()
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data['uri'] ==~ "/api/memberships/.*/"
        }
    }

    void "it should return accepted when membership is created successfully by a collaborator"() {
        given:
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and:
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.WRITE, Resource.MEMBERSHIP)] as Set
        )
        def info = MembershipInformation.of("bla", "bla", 10, ["SATURDAY"] as Set)
        def token = authUtil.createAndLoginUser(
                collaborator.email(),
                "1234"
        )

        when:
        def response = mockMvc.perform(post("/api/office_branches/${officeBranch.id()}/memberships/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info))
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.CREATED.value()
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data['uri'] ==~ "/api/memberships/.*/"
        }
    }
}

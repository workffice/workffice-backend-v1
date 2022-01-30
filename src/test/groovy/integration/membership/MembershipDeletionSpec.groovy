package integration.membership

import backoffice.application.dto.membership.MembershipInformation
import backoffice.domain.membership.MembershipId
import backoffice.domain.membership.MembershipRepository
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.domain.role.Access
import backoffice.domain.role.Permission
import backoffice.domain.role.Resource
import backoffice.factories.MembershipBuilder
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class MembershipDeletionSpec extends Specification {
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    MembershipRepository membershipRepo
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
        def response = mockMvc.perform(delete("/api/memberships/1/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info)))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return bad request when membership id is not uuid format"() {
        given:
        def info = MembershipInformation.of("bla", "bla", 10, ["SATURDAY"] as Set)
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(delete("/api/memberships/1/")
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
                            'error'  : 'INVALID_MEMBERSHIP_ID',
                            'message': 'The membership id provided is invalid',
                    ]
            ]
        }
    }

    void "it should return not found when membership does not exist"() {
        given:
        def membershipId = new MembershipId()
        def info = MembershipInformation.of("bla", "bla", 10, ["SATURDAY"] as Set)
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(delete("/api/memberships/${membershipId}/")
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
                            'error'  : 'MEMBERSHIP_NOT_FOUND',
                            'message': 'The membership requested does not exist',
                    ]
            ]
        }
    }

    void "it should return forbidden when auth user is not owner of membership"() {
        given:
        def membership = new MembershipBuilder().build()
        officeHolderRepo.store(membership.officeBranch().owner())
        officeBranchRepo.store(membership.officeBranch())
        membershipRepo.store(membership)
        and:
        def info = MembershipInformation.of("bla", "bla", 10, ["SATURDAY"] as Set)
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(delete("/api/memberships/${membership.id()}/")
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
        def membership = new MembershipBuilder().build()
        officeHolderRepo.store(membership.officeBranch().owner())
        officeBranchRepo.store(membership.officeBranch())
        membershipRepo.store(membership)
        and:
        def collaborator = collaboratorUtil.createCollaborator(
                membership.officeBranch(),
                [Permission.create(Access.WRITE, Resource.NEWS)] as Set
        )
        def info = MembershipInformation.of("bla", "bla", 10, ["SATURDAY"] as Set)
        def token = authUtil.createAndLoginUser(
                collaborator.email(),
                "1234"
        )

        when:
        def response = mockMvc.perform(delete("/api/memberships/${membership.id()}/")
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

    void "it should return accepted when membership is deleted successfully"() {
        given:
        def membership = new MembershipBuilder().build()
        officeHolderRepo.store(membership.officeBranch().owner())
        officeBranchRepo.store(membership.officeBranch())
        membershipRepo.store(membership)
        and:
        def info = MembershipInformation.of("bla", "bla", 10, ["SATURDAY"] as Set)
        def token = authUtil.createAndLoginUser(
                membership.officeBranch().owner().email(),
                "1234"
        )

        when:
        def response = mockMvc.perform(delete("/api/memberships/${membership.id()}/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info))
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.ACCEPTED.value()
    }

    void "it should return accepted when membership is deleted successfully by a collaborator"() {
        given:
        def membership = new MembershipBuilder().build()
        officeHolderRepo.store(membership.officeBranch().owner())
        officeBranchRepo.store(membership.officeBranch())
        membershipRepo.store(membership)
        and:
        def collaborator = collaboratorUtil.createCollaborator(
                membership.officeBranch(),
                [Permission.create(Access.WRITE, Resource.MEMBERSHIP)] as Set
        )
        def info = MembershipInformation.of("bla", "bla", 10, ["SATURDAY"] as Set)
        def token = authUtil.createAndLoginUser(
                collaborator.email(),
                "1234"
        )

        when:
        def response = mockMvc.perform(delete("/api/memberships/${membership.id()}/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info))
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.ACCEPTED.value()
    }
}

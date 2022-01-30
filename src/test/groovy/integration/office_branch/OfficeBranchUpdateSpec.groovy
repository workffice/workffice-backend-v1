package integration.office_branch

import authentication.application.dto.token.Authentication
import backoffice.application.dto.office_branch.OfficeBranchUpdateInformation
import backoffice.domain.office_branch.Image
import backoffice.domain.office_branch.OfficeBranchId
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.factories.OfficeBranchBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.javafaker.Faker
import controller.AuthUtil
import controller.CollaboratorUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import server.WorkfficeApplication
import shared.domain.EventBus
import spock.lang.Specification

import static controller.IntegrationTestUtil.asJsonString
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class OfficeBranchUpdateSpec extends Specification {
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
    @MockBean
    EventBus eventBus

    Faker faker = Faker.instance()

    private static MockHttpServletRequestBuilder updateOfficeBranchRequest(
            String id,
            Authentication token,
            OfficeBranchUpdateInformation officeBranchUpdateInfo
    ) {
        put("/api/office_branches/${id}/")
                .header("Authorization", "Bearer ${token.getToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(officeBranchUpdateInfo))
    }

    void "it should return not authorized when token is not provided"() {
        given:
        def officeBranchUpdateInfo = OfficeBranchUpdateInformation.of(
                "some name",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
        )

        when:
        def response = mockMvc.perform(put("/api/office_branches/1/")
                .contentType(MediaType.APPLICATION_JSON)
                .contentType(asJsonString(officeBranchUpdateInfo)))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return bad request when office branch id is not uuid format"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def officeBranchUpdateInfo = OfficeBranchUpdateInformation.of(
                "some name",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
        )

        when:
        def response = mockMvc.perform(updateOfficeBranchRequest("-1", token, officeBranchUpdateInfo))
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
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def officeBranchId = new OfficeBranchId()
        def officeBranchUpdateInfo = OfficeBranchUpdateInformation.of(
                "some name",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
        )

        when:
        def response = mockMvc.perform(updateOfficeBranchRequest(officeBranchId.toString(), token, officeBranchUpdateInfo))
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
        given: 'An office branch'
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and: 'An authenticated user that is not the owner of the office branch'
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def officeBranchUpdateInfo = OfficeBranchUpdateInformation.of(
                "some name",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
        )

        when:
        def response = mockMvc.perform(updateOfficeBranchRequest(officeBranch.id().toString(), token, officeBranchUpdateInfo))
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


    void "it should return ok when office branch is updated successfully"() {
        given: 'An office branch'
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and: 'An authenticated user that is the owner of the office branch'
        def token = authUtil.createAndLoginUser(officeBranch.owner().email(), "1234")
        def officeBranchUpdateInfo = OfficeBranchUpdateInformation.of(
                "Monumental",
                null,
                null,
                ["coco.com", "some.io"],
                null,
                null,
                "Volcan Santa Maria",
                null,
        )

        when:
        def response = mockMvc.perform(updateOfficeBranchRequest(officeBranch.id().toString(), token, officeBranchUpdateInfo))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        def officeBranchUpdated = officeBranchRepo.findById(officeBranch.id()).get()
        officeBranchUpdated.name() == "Monumental"
        officeBranchUpdated.location().street() == "Volcan Santa Maria"
        officeBranchUpdated.images() as Set == [new Image('coco.com'), new Image('some.io')] as Set
    }
}

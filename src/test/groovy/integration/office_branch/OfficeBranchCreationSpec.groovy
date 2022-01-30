package integration.office_branch

import authentication.application.dto.token.Authentication
import backoffice.application.dto.office_branch.OfficeBranchInformation
import backoffice.domain.office_holder.OfficeHolder
import backoffice.domain.office_holder.OfficeHolderId
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.factories.OfficeHolderBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.javafaker.Faker
import controller.AuthUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import server.WorkfficeApplication
import spock.lang.Specification

import static controller.IntegrationTestUtil.asJsonString
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class OfficeBranchCreationSpec extends Specification {
    @Autowired
    MockMvc mockMvc
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    AuthUtil authUtil
    @Autowired
    ObjectMapper objectMapper

    def faker = Faker.instance()

    static OfficeBranchInformation example(String name) {
        return OfficeBranchInformation.of(
                name,
                "some desc",
                "263331111",
                Arrays.asList("imageurl1", "imageurl2"),
                "Mendoza",
                "Godoy Cruz",
                "VSM",
                "5501"
        )
    }

    static MockHttpServletRequestBuilder officeBranchCreationRequest(
            String officeHolderId,
            OfficeBranchInformation information,
            Authentication authentication
    ) {
        return post("/api/office_holders/${officeHolderId}/office_branches/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(information))
                .header("Authorization", "Bearer " + authentication.getToken())
    }

    void "it should return bad request when office branch id has not uuid format"() {
        given: 'An authenticated user'
        Authentication authentication = authUtil.createAndLoginUser("scaloneta@doe.com", "1234")
        OfficeBranchInformation information = example(faker.name().name())

        when: 'Pass an id incompatible with uuid'
        def response = mockMvc.perform(officeBranchCreationRequest("-1", information, authentication))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INVALID_ID',
                            'message': 'Id provided is invalid'
                    ]
            ]
        }
    }

    void 'it should return not found when office holder does not exist'() {
        given: 'An authenticated user accessing'
        OfficeBranchInformation information = example(faker.name().name())
        Authentication authentication = authUtil.createAndLoginUser("scaloneta@doe.com", "1234")

        when: 'Trying to create an office branch for an nonexistent office holder id'
        String officeHolderId = new OfficeHolderId().toString()
        def response = mockMvc.perform(officeBranchCreationRequest(officeHolderId, information, authentication))
                .andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'NOT_FOUND',
                            'error'  : 'OFFICE_HOLDER_NOT_FOUND',
                            'message': 'There is no office holder with specified id'
                    ]
            ]
        }
    }

    void "it should return forbidden when auth user has no access to office holder"() {
        given: 'A office holder with email gallardo@doe.com'
        OfficeHolder officeHolder = new OfficeHolderBuilder()
                .withEmail('gallardo@doe.com').build()
        officeHolderRepo.store(officeHolder)
        and: 'An authenticated user with email scaloneta@doe.com'
        OfficeBranchInformation information = example(faker.name().name())
        Authentication authentication = authUtil.createAndLoginUser("scaloneta@doe.com", "1234");

        when: 'Auth user tries to create an office branch for another office holder'
        def response = mockMvc
                .perform(officeBranchCreationRequest(officeHolder.id().toString(), information, authentication))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'OFFICE_HOLDER_FORBIDDEN',
                            'message': 'You do not have access to this resource'
                    ]
            ]
        }
    }

    void 'it should return created when user can create a new office branch successfully'() {
        given: 'An office holder'
        OfficeHolder officeHolder = new OfficeHolderBuilder().build()
        officeHolderRepo.store(officeHolder)
        and: 'An authenticated user with same email as office holder'
        Authentication authentication = authUtil.createAndLoginUser(officeHolder.email(), "1234")

        when:
        OfficeBranchInformation information = example(faker.name().name())
        def response = mockMvc
                .perform(officeBranchCreationRequest(officeHolder.id().toString(), information, authentication))
                .andReturn().response

        then:
        response.status == HttpStatus.CREATED.value()
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data['uri'] ==~ "/api/office_branches/.*/"
        }
    }
}

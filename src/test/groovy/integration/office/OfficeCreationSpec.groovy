package integration.office

import authentication.application.dto.token.Authentication
import backoffice.application.dto.office.OfficeInformation
import backoffice.domain.office_branch.OfficeBranch
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import server.WorkfficeApplication
import spock.lang.Shared
import spock.lang.Specification

import static controller.IntegrationTestUtil.asJsonString
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class OfficeCreationSpec extends Specification {
    @Autowired
    MockMvc mockMvc
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    AuthUtil authUtil
    @Autowired
    CollaboratorUtil collaboratorUtil
    @Autowired
    ObjectMapper objectMapper

    def faker = Faker.instance()
    @Shared
    def missingName = [
            'code'   : 'INVALID',
            'error'  : 'INVALID_NAME',
            'message': 'Name is required'
    ]
    @Shared
    def notNullDescription = [
            'code'   : 'INVALID',
            'error'  : 'INVALID_DESCRIPTION',
            'message': 'Description must not be null'
    ]
    @Shared
    def missingCapacity = [
            'code'   : 'INVALID',
            'error'  : 'INVALID_CAPACITY',
            'message': 'Capacity is required'
    ]
    @Shared
    def missingPrice = [
            'code'   : 'INVALID',
            'error'  : 'INVALID_PRICE',
            'message': 'Price is required'
    ]
    @Shared
    def missingPrivacy = [
            'code'   : 'INVALID',
            'error'  : 'INVALID_PRIVACY',
            'message': 'Privacy is required'
    ]
    @Shared
    def invalidPrivacy = [
            'code'   : 'INVALID',
            'error'  : 'INVALID_PRIVACY',
            'message': 'Privacy must be PRIVATE or SHARED'
    ]

    OfficeInformation officeInformationExample() {
        return OfficeInformation.of(
                faker.name().name(),
                faker.lorem().paragraph(),
                10,
                100,
                "SHARED",
                faker.internet().image(),
                10,
                10
        )
    }

    static MockHttpServletRequestBuilder officeCreationRequest(
            String officeBranchId,
            OfficeInformation information,
            Authentication authentication
    ) {
        return post("/api/office_branches/${officeBranchId}/offices/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(information))
                .header("Authorization", "Bearer " + authentication.getToken())
    }

    void 'it should return forbidden when auth user does not own office branch'() {
        given: 'An existent office branch'
        OfficeBranch officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch).get()
        and: 'An authenticated user with different email than office holder'
        def authentication = authUtil.createAndLoginUser("no.auth@mail.com", "1")

        when:
        OfficeInformation officeInformation = officeInformationExample()
        def response = mockMvc
                .perform(officeCreationRequest(
                        officeBranch.id().toString(),
                        officeInformation,
                        authentication))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
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

    void 'it should return forbidden when collaborator does not have write access to office resource for a office branch'() {
        given: 'An existent office branch'
        OfficeBranch officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch).get()
        and: 'An authenticated collaborator without write access to office resource'
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.READ, Resource.ROLE)] as Set
        )
        def authentication = authUtil.createAndLoginUser(collaborator.email(), "1")

        when:
        OfficeInformation officeInformation = officeInformationExample()
        def response = mockMvc
                .perform(officeCreationRequest(
                        officeBranch.id().toString(),
                        officeInformation,
                        authentication))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
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

    void 'it should return not found when office branch does not exist'() {
        given: 'An authenticated user'
        var authentication = authUtil.createAndLoginUser("mmm@mail.com", "1234")

        when: 'User tries to create an office for an nonexistent office branch'
        OfficeInformation officeInformation = officeInformationExample()
        OfficeBranchId nonExistentOfficeBranchId = new OfficeBranchId()
        def response = mockMvc
                .perform(officeCreationRequest(
                        nonExistentOfficeBranchId.toString(),
                        officeInformation,
                        authentication))
                .andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()
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

    void 'it should return bad request when office is shared and tables are not specified'() {
        given: 'An existent office branch'
        OfficeBranch officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch).get()
        and: 'An authenticated user'
        Authentication authentication = authUtil
                .createAndLoginUser(officeBranch.owner().email(), "1")
        and: 'A new shared office with no tables specified'
        OfficeInformation officeInformation = OfficeInformation.of(
                faker.name().name(),
                faker.lorem().paragraph(),
                10,
                100,
                "SHARED",
                faker.internet().image(),
                null,
                10
        )

        when:
        def response = mockMvc
                .perform(officeCreationRequest(
                        officeBranch.id().toString(),
                        officeInformation,
                        authentication))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'SHARED_OFFICE_WITHOUT_TABLES',
                            'message': 'You must specify table information for a shared office'
                    ]
            ]
        }
    }

    void 'it should return bad request when office branch does not have uuid format'() {
        given: 'An authenticated userr'
        var officeInformation = officeInformationExample()
        var authentication = authUtil
                .createAndLoginUser("john@doe.com", "1")

        when: 'User tries to create an office for an invalid office branch id'
        def response = mockMvc
                .perform(officeCreationRequest(
                        "-1",
                        officeInformation,
                        authentication))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
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

    void 'it should return created when user creates an office successfully'() {
        given: 'An authenticated user'
        var officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch).get()
        var authentication = authUtil
                .createAndLoginUser(officeBranch.owner().email(), "1234")

        when:
        def officeInformation = OfficeInformation.of(
                faker.name().name(),
                faker.lorem().paragraph(),
                10,
                100,
                privacy,
                faker.internet().image(),
                tablesQuantity,
                capacityPerTable
        )
        def response = mockMvc
                .perform(officeCreationRequest(
                        officeBranch.id().toString(),
                        officeInformation,
                        authentication))
                .andReturn().response

        then:
        response.status == HttpStatus.CREATED.value()
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data['uri'] ==~ "/api/offices/.*/"
        }

        where:
        privacy   | tablesQuantity | capacityPerTable
        'SHARED'  | 10             | 10
        'PRIVATE' | null           | null
        'PRIVATE' | null           | 10
        'PRIVATE' | 10             | null
    }

    void 'it should return created when collaborator with write access to office resource for an office branch tries to create an office'() {
        given: 'An office branch'
        var officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch).get()
        and: 'An authenticated collaborator with write access to office resource for the office branch'
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.WRITE, Resource.OFFICE)] as Set
        )
        var authentication = authUtil
                .createAndLoginUser(collaborator.email(), "1234")

        when:
        def officeInformation = OfficeInformation.of(
                faker.name().name(),
                faker.lorem().paragraph(),
                10,
                100,
                "SHARED",
                faker.internet().image(),
                10,
                10
        )
        def response = mockMvc
                .perform(officeCreationRequest(
                        officeBranch.id().toString(),
                        officeInformation,
                        authentication))
                .andReturn().response

        then:
        response.status == HttpStatus.CREATED.value()
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data['uri'] ==~ "/api/offices/.*/"
        }
    }

    void "it should return bad request when office information is incomplete or invalid"() {
        given: 'An authenticated user'
        var officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch).get()
        var authentication = authUtil
                .createAndLoginUser(officeBranch.owner().email(), "1234")

        when:
        def officeInformation = OfficeInformation.of(
                name,
                description,
                capacity,
                price,
                privacy,
                faker.internet().image(),
                10,
                10
        )
        def response = mockMvc
                .perform(officeCreationRequest(
                        officeBranch.id().toString(),
                        officeInformation,
                        authentication))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors as Set == expectedErrors as Set
        }

        where:
        name  | description | capacity | price | privacy || expectedErrors
        ''    | null        | 10       | 10    | 'S'     || [missingName, notNullDescription, invalidPrivacy]
        'Bla' | 'Some desc' | null     | null  | ''      || [missingCapacity, missingPrice, missingPrivacy]
    }
}

package integration.office_branch

import backoffice.domain.collaborator.CollaboratorRepository
import backoffice.domain.collaborator.Status
import backoffice.domain.office_branch.Image
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.factories.CollaboratorBuilder
import backoffice.factories.OfficeBranchBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.javafaker.Faker
import com.google.common.collect.ImmutableList
import controller.AuthUtil
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
class OfficeBranchesFinderByCollaboratorSpec extends Specification {
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    CollaboratorRepository collaboratorRepo
    @Autowired
    AuthUtil authUtil
    @Autowired
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper

    Faker faker = Faker.instance()

    void "it should return not authorized when token is not provided"() {
        when:
        def response = mockMvc
                .perform(get("/api/office_branches/?collaborator_email='john@doe.com'"))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return forbidden when auth user does not have the collaborator email specified"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc
                .perform(get("/api/office_branches/?collaborator_email='john@doe.com'")
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
                            'message': 'You don\'t have access to collaborator office branches',
                    ]
            ]
        }
    }

    void "it should return ok with empty office branches when there is no collaborator with email provided"() {
        given:
        def email = faker.internet().emailAddress()
        def token = authUtil.createAndLoginUser(email, "1234")

        when:
        def response = mockMvc
                .perform(get("/api/office_branches/?collaborator_email=${email}")
                        .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == []
        }
    }

    void "it should return ok with office branches related with collaborators with email specified"() {
        given: 'Three collaborators with the same email for different office branches'
        def email = faker.internet().emailAddress()
        def officeBranch1 = new OfficeBranchBuilder()
                .withImages(ImmutableList.of(new Image("1.com"), new Image("2.com")))
                .build()
        def collaborator1 = new CollaboratorBuilder()
                .withStatus(Status.ACTIVE)
                .withOfficeBranch(officeBranch1)
                .withEmail(email).build()
        def collaborator2 = new CollaboratorBuilder()
                .withStatus(Status.ACTIVE)
                .withEmail(email).build()
        def collaborator3 = new CollaboratorBuilder()
                .withStatus(Status.ACTIVE).withEmail(email).build()
        officeHolderRepo.store(collaborator1.officeBranch().owner())
        officeHolderRepo.store(collaborator2.officeBranch().owner())
        officeHolderRepo.store(collaborator3.officeBranch().owner())
        officeBranchRepo.store(collaborator1.officeBranch())
        officeBranchRepo.store(collaborator2.officeBranch())
        officeBranchRepo.store(collaborator3.officeBranch())
        collaboratorRepo.store(collaborator1)
        collaboratorRepo.store(collaborator2)
        collaboratorRepo.store(collaborator3)
        and: 'And an authenticated user with the collaborator email'
        def token = authUtil.createAndLoginUser(email, "1234")

        when:
        def response = mockMvc
                .perform(get("/api/office_branches/?collaborator_email=${email}")
                        .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data.size() == 3
            it.data as Set == [
                    [
                            'id'         : collaborator1.officeBranch().toResponse().id,
                            'name'       : collaborator1.officeBranch().toResponse().name,
                            'description': collaborator1.officeBranch().toResponse().description,
                            'phone'      : collaborator1.officeBranch().toResponse().phone,
                            'created'    : collaborator1.officeBranch().toResponse().created.toString(),
                            'images'     : [
                                    ["url": "1.com"],
                                    ["url": "2.com"]
                            ],
                            'location'   : [
                                    'city'    : collaborator1.officeBranch().toResponse().location.city,
                                    'street'  : collaborator1.officeBranch().toResponse().location.street,
                                    'province': collaborator1.officeBranch().toResponse().location.province,
                                    'zipCode' : collaborator1.officeBranch().toResponse().location.zipCode
                            ]
                    ],
                    [
                            'id'         : collaborator2.officeBranch().toResponse().id,
                            'name'       : collaborator2.officeBranch().toResponse().name,
                            'description': collaborator2.officeBranch().toResponse().description,
                            'phone'      : collaborator2.officeBranch().toResponse().phone,
                            'created'    : collaborator2.officeBranch().toResponse().created.toString(),
                            'images'     : [],
                            'location'   : [
                                    'city'    : collaborator2.officeBranch().toResponse().location.city,
                                    'street'  : collaborator2.officeBranch().toResponse().location.street,
                                    'province': collaborator2.officeBranch().toResponse().location.province,
                                    'zipCode' : collaborator2.officeBranch().toResponse().location.zipCode
                            ]
                    ],
                    [
                            'id'         : collaborator3.officeBranch().toResponse().id,
                            'name'       : collaborator3.officeBranch().toResponse().name,
                            'description': collaborator3.officeBranch().toResponse().description,
                            'phone'      : collaborator3.officeBranch().toResponse().phone,
                            'created'    : collaborator3.officeBranch().toResponse().created.toString(),
                            'images'     : [],
                            'location'   : [
                                    'city'    : collaborator3.officeBranch().toResponse().location.city,
                                    'street'  : collaborator3.officeBranch().toResponse().location.street,
                                    'province': collaborator3.officeBranch().toResponse().location.province,
                                    'zipCode' : collaborator3.officeBranch().toResponse().location.zipCode
                            ]
                    ],
            ] as Set
        }
    }
}

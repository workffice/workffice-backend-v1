package integration.news

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
import news.application.dto.NewsInfo
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
class NewsCreationSpec extends Specification {
    @Autowired
    MockMvc mockMvc
    @Autowired
    AuthUtil authUtil
    @Autowired
    CollaboratorUtil collaboratorUtil
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    ObjectMapper objectMapper

    Faker faker = Faker.instance()

    void "it should return not authorized when token is not provided"() {
        given:
        def info = NewsInfo.of("subject", "title", "body")

        when:
        def response = mockMvc.perform(post("/api/office_branches/123/news/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info))
        ).andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return not found when there is no office branch with id provided"() {
        given:
        def info = NewsInfo.of("subject", "title", "body")
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def officeBranchId = UUID.randomUUID().toString()

        when:
        def response = mockMvc.perform(post("/api/office_branches/${officeBranchId}/news/")
                .header("Authorization", "Bearer ${token.getToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info))
        ).andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'NOT_FOUND',
                            'error'  : 'OFFICE_BRANCH_NOT_FOUND',
                            'message': 'There is no office branch with id specified',
                    ]
            ]
        }
    }

    void "it should return forbidden when auth user is not owner of office branch news"() {
        given:
        def info = NewsInfo.of("subject", "title", "body")
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)

        when:
        def response = mockMvc.perform(post("/api/office_branches/${officeBranch.id()}/news/")
                .header("Authorization", "Bearer ${token.getToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info))
        ).andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'NEWS_FORBIDDEN',
                            'message': 'You don\'t have access to news',
                    ]
            ]
        }
    }

    void "it should return forbidden when auth user is a collaborator without office branch news permissions"() {
        given:
        def info = NewsInfo.of("subject", "title", "body")
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and:
        def collaborator = collaboratorUtil.createCollaborator(officeBranch, [Permission.create(Access.WRITE, Resource.OFFICE)] as Set)
        def token = authUtil.createAndLoginUser(collaborator.email(), "1234")

        when:
        def response = mockMvc.perform(post("/api/office_branches/${officeBranch.id()}/news/")
                .header("Authorization", "Bearer ${token.getToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info))
        ).andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'NEWS_FORBIDDEN',
                            'message': 'You don\'t have access to news',
                    ]
            ]
        }
    }

    void "it should return created when news is created successfully"() {
        given:
        def info = NewsInfo.of("subject", "title", "body")
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and:
        def token = authUtil.createAndLoginUser(officeBranch.owner().email(), "1234")

        when:
        def response = mockMvc.perform(post("/api/office_branches/${officeBranch.id()}/news/")
                .header("Authorization", "Bearer ${token.getToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info))
        ).andReturn().response

        then:
        response.status == HttpStatus.CREATED.value()
    }

    void "it should return created when news is created successfully by a collaborator"() {
        given:
        def info = NewsInfo.of("subject", "title", "body")
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        and:
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.WRITE, Resource.OFFICE), Permission.create(Access.WRITE, Resource.NEWS)] as Set
        )
        def token = authUtil.createAndLoginUser(collaborator.email(), "1234")

        when:
        def response = mockMvc.perform(post("/api/office_branches/${officeBranch.id()}/news/")
                .header("Authorization", "Bearer ${token.getToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info))
        ).andReturn().response

        then:
        response.status == HttpStatus.CREATED.value()
    }
}

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
import news.domain.NewsRepository
import news.factory.NewsBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import server.WorkfficeApplication
import spock.lang.Specification

import static controller.IntegrationTestUtil.asJsonString
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class NewsUpdaterSpec extends Specification {
    @Autowired
    MockMvc mockMvc
    @Autowired
    NewsRepository newsRepo
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    AuthUtil authUtil
    @Autowired
    CollaboratorUtil collaboratorUtil
    @Autowired
    ObjectMapper objectMapper
    @Autowired
    MongoTemplate mongoTemplate

    Faker faker = Faker.instance()

    def cleanup() {
        var collection = mongoTemplate.getCollection("news")
        collection.drop()
    }

    void "it should return not authorized when token is not provided"() {
        given:
        def info = NewsInfo.of("New subject", "New title", "New body")

        when:
        def response = mockMvc.perform(put("/api/news/1/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info)))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return not found when news does not exist"() {
        given:
        def info = NewsInfo.of("New subject", "New title", "New body")
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(put("/api/news/1/")
                .header("Authorization", "Bearer ${token.getToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info)))
                .andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'NOT_FOUND',
                            'error'  : 'NEWS_NOT_FOUND',
                            'message': 'There is not news with id provided',
                    ]
            ]
        }
    }

    void "it should return forbidden when auth user is not owner of news"() {
        given:
        def info = NewsInfo.of("New subject", "New title", "New body")
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        def news = new NewsBuilder()
                .withOfficeBranchId(officeBranch.id().toString())
                .build()
        newsRepo.store(news).get()
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(put("/api/news/${news.id()}/")
                .header("Authorization", "Bearer ${token.getToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info)))
                .andReturn().response

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

    void "it should return forbidden when collaborator has no enough perms to edit news"() {
        given:
        def info = NewsInfo.of("New subject", "New title", "New body")
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        def news = new NewsBuilder()
                .withOfficeBranchId(officeBranch.id().toString())
                .build()
        newsRepo.store(news)
        and:
        def collaborator = collaboratorUtil.createCollaborator(officeBranch, [Permission.create(Access.READ, Resource.BOOKING)] as Set)
        def token = authUtil.createAndLoginUser(collaborator.email(), "1234")

        when:
        def response = mockMvc.perform(put("/api/news/${news.id()}/")
                .header("Authorization", "Bearer ${token.getToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info)))
                .andReturn().response

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

    void "it should return bad request when news is not in draft status"() {
        given:
        def info = NewsInfo.of("New subject", "New title", "New body")
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        def news = new NewsBuilder()
                .withOfficeBranchId(officeBranch.id().toString())
                .build()
        news.delete()
        newsRepo.store(news)
        and:
        def token = authUtil.createAndLoginUser(officeBranch.owner().email(), "1234")

        when:
        def response = mockMvc.perform(put("/api/news/${news.id()}/")
                .header("Authorization", "Bearer ${token.getToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info)))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'NEWS_IS_NOT_DRAFT',
                            'message': 'News is not in draft status',
                    ]
            ]
        }
    }

    void "it should return accepted when news is updated successfully"() {
        given:
        def info = NewsInfo.of("New subject", "New title", "New body")
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        def news = new NewsBuilder()
                .withOfficeBranchId(officeBranch.id().toString())
                .build()
        newsRepo.store(news)
        and:
        def token = authUtil.createAndLoginUser(officeBranch.owner().email(), "1234")

        when:
        def response = mockMvc.perform(put("/api/news/${news.id()}/")
                .header("Authorization", "Bearer ${token.getToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info)))
                .andReturn().response

        then:
        response.status == HttpStatus.ACCEPTED.value()
    }

    void "it should return accepted when news is updated successfully by a collaborator with right perms"() {
        given:
        def info = NewsInfo.of("New subject", "New title", "New body")
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        def news = new NewsBuilder()
                .withOfficeBranchId(officeBranch.id().toString())
                .build()
        newsRepo.store(news)
        and:
        def collaborator = collaboratorUtil.createCollaborator(
                officeBranch,
                [Permission.create(Access.WRITE, Resource.NEWS)] as Set
        )
        def token = authUtil.createAndLoginUser(collaborator.email(), "1234")

        when:
        def response = mockMvc.perform(put("/api/news/${news.id()}/")
                .header("Authorization", "Bearer ${token.getToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(info)))
                .andReturn().response

        then:
        response.status == HttpStatus.ACCEPTED.value()
    }
}

package integration.news

import com.fasterxml.jackson.databind.ObjectMapper
import news.domain.NewsRepository
import news.factory.NewsBuilder
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
class NewsFinderSpec extends Specification {
    @Autowired
    MockMvc mockMvc
    @Autowired
    NewsRepository newsRepo
    @Autowired
    ObjectMapper objectMapper

    void "it should return ok and news related with office branches"() {
        given:
        def officeBranchId = UUID.randomUUID().toString()
        def news1 = new NewsBuilder()
                .withOfficeBranchId(officeBranchId)
                .build()
        def news2 = new NewsBuilder()
                .withOfficeBranchId(officeBranchId)
                .build()
        news2.markAsSent(["some@email.com", "napoleon@email.com"])
        def news3 = new NewsBuilder()
                .withOfficeBranchId(officeBranchId)
                .build()
        newsRepo.store(news1)
        newsRepo.store(news2)
        newsRepo.store(news3)

        when:
        def response = mockMvc.perform(get("/api/office_branches/${officeBranchId}/news/"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data as Set == [
                    [
                            'id'        : news1.toResponse().id,
                            'subject'   : news1.toResponse().subject,
                            'title'     : news1.toResponse().title,
                            'body'      : news1.toResponse().body,
                            'created'   : news1.toResponse().created.toString(),
                            'status'    : news1.toResponse().status,
                            'sentAt'    : null,
                            'recipients': [],
                    ],
                    [
                            'id'        : news2.toResponse().id,
                            'subject'   : news2.toResponse().subject,
                            'title'     : news2.toResponse().title,
                            'body'      : news2.toResponse().body,
                            'created'   : news2.toResponse().created.toString(),
                            'status'    : news2.toResponse().status,
                            'sentAt'    : news2.toResponse().sentAt.toString() + ":00",
                            'recipients': news2.toResponse().recipients.toList(),
                    ],
                    [
                            'id'        : news3.toResponse().id,
                            'subject'   : news3.toResponse().subject,
                            'title'     : news3.toResponse().title,
                            'body'      : news3.toResponse().body,
                            'created'   : news3.toResponse().created.toString(),
                            'status'    : news3.toResponse().status,
                            'sentAt'    : null,
                            'recipients': [],
                    ],
            ] as Set
        }
    }
}

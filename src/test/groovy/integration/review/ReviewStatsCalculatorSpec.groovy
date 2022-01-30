package integration.review

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import review.domain.office.Office
import review.domain.office.OfficeRepository
import server.WorkfficeApplication
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class ReviewStatsCalculatorSpec extends Specification {
    @Autowired
    OfficeRepository officeRepo
    @Autowired
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper
    @Autowired
    MongoTemplate mongoTemplate

    def cleanup() {
        var collection = mongoTemplate.getCollection("reviews")
        collection.drop()
    }

    void "it should return ok with empty stats when office branch has no reviews"() {
        when:
        def officeBranchId = UUID.randomUUID().toString()
        def response = mockMvc.perform(get("/api/office_branches/${officeBranchId}/review_stats/"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == [
                    'id'          : officeBranchId,
                    'totalVotes'  : 0,
                    'averageStars': 0,
            ]
        }
    }

    void "it should return ok stats for office branch"() {
        given:
        def officeBranchId = UUID.randomUUID().toString()
        def office1 = Office.create("1", officeBranchId, 15, 3)
        def office2 = Office.create("2", officeBranchId, 10, 10)
        def office3 = Office.create("3", officeBranchId, 3, 1)
        officeRepo.save(office1)
        officeRepo.save(office2)
        officeRepo.save(office3)

        when:
        def response = mockMvc.perform(get("/api/office_branches/${officeBranchId}/review_stats/"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == [
                    'id'          : officeBranchId,
                    'totalVotes'  : 14,
                    'averageStars': (15 / 3 + 10 / 10 + 3 / 1) / 3,
            ]
        }
    }
}

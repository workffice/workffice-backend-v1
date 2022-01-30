package integration.review

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import review.domain.review.ReviewRepository
import review.factories.ReviewBuilder
import server.WorkfficeApplication
import spock.lang.Specification

import java.time.LocalDateTime

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class ReviewFinderSpec extends Specification {
    @Autowired
    ReviewRepository reviewRepo
    @Autowired
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper

    void "it should return reviews ordered from the last one to the oldest one"() {
        given:
        def officeId = UUID.randomUUID().toString()
        def review1 = new ReviewBuilder()
                .withOfficeId(officeId)
                .withCreated(LocalDateTime.of(2021, 9, 11, 14, 0))
                .build()
        def review2 = new ReviewBuilder()
                .withOfficeId(officeId)
                .withCreated(LocalDateTime.of(2021, 8, 11, 14, 0))
                .build()
        def review3 = new ReviewBuilder()
                .withOfficeId(officeId)
                .withCreated(LocalDateTime.of(2021, 9, 11, 12, 0))
                .build()
        reviewRepo.store(review1)
        reviewRepo.store(review2)
        reviewRepo.store(review3)

        when:
        def response = mockMvc.perform(get("/api/offices/${officeId}/reviews/"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == [
                    [
                            'comment'    : review1.toResponse().comment,
                            'stars'      : review1.toResponse().stars,
                            'renterEmail': review1.toResponse().renterEmail,
                            'created'    : review1.toResponse().created.toString() + ":00",
                    ],
                    [
                            'comment'    : review3.toResponse().comment,
                            'stars'      : review3.toResponse().stars,
                            'renterEmail': review3.toResponse().renterEmail,
                            'created'    : review3.toResponse().created.toString() + ":00",
                    ],
                    [
                            'comment'    : review2.toResponse().comment,
                            'stars'      : review2.toResponse().stars,
                            'renterEmail': review2.toResponse().renterEmail,
                            'created'    : review2.toResponse().created.toString() + ":00",
                    ],
            ]
        }
    }

    void "it should paginate reviews"() {
        given:
        def officeId = UUID.randomUUID().toString()
        def review1 = new ReviewBuilder()
                .withOfficeId(officeId)
                .withCreated(LocalDateTime.of(2021, 9, 11, 14, 0))
                .build()
        def review2 = new ReviewBuilder()
                .withOfficeId(officeId)
                .withCreated(LocalDateTime.of(2021, 8, 11, 14, 0))
                .build()
        def review3 = new ReviewBuilder()
                .withOfficeId(officeId)
                .withCreated(LocalDateTime.of(2021, 9, 11, 12, 0))
                .build()
        def review4 = new ReviewBuilder()
                .withOfficeId(officeId)
                .withCreated(LocalDateTime.of(2021, 8, 10, 12, 0))
                .build()
        reviewRepo.store(review1)
        reviewRepo.store(review2)
        reviewRepo.store(review3)
        reviewRepo.store(review4)

        when:
        def response = mockMvc.perform(get("/api/offices/${officeId}/reviews/?page=1&size=2"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == [
                    [
                            'comment'    : review2.toResponse().comment,
                            'stars'      : review2.toResponse().stars,
                            'renterEmail': review2.toResponse().renterEmail,
                            'created'    : review2.toResponse().created.toString() + ":00",
                    ],
                    [
                            'comment'    : review4.toResponse().comment,
                            'stars'      : review4.toResponse().stars,
                            'renterEmail': review4.toResponse().renterEmail,
                            'created'    : review4.toResponse().created.toString() + ":00",
                    ],
            ]
            it.pagination == [
                    'pageSize'   : 2,
                    'lastPage'   : false,
                    'totalPages' : -1,
                    'currentPage': 1,
            ]
        }
    }
}

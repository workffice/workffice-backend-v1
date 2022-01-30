package integration.office

import backoffice.domain.office.OfficeId
import backoffice.domain.office.OfficeRepository
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.factories.OfficeBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import server.WorkfficeApplication
import spock.lang.Specification

import java.time.Clock
import java.time.LocalDate

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class OfficeFinderSpec extends Specification {
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    OfficeRepository officeRepo
    @Autowired
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper

    void "it should return bad request when id provided is not uuid format"() {
        when:
        def response = mockMvc
                .perform(get("/api/offices/-1/"))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INVALID_OFFICE_ID',
                            'message': 'Office id provided has a wrong format'
                    ]
            ]
        }
    }

    void "it should return not found when there is no office with id provided"() {
        given: 'A valid office id but it does not exist'
        def nonExistentOfficeId = new OfficeId()

        when:
        def response = mockMvc
                .perform(get("/api/offices/${nonExistentOfficeId}/"))
                .andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'NOT_FOUND',
                            'error'  : 'OFFICE_NOT_FOUND',
                            'message': 'There is no office with id provided',
                    ]
            ]
        }
    }

    void "it should return not found when office is deleted"() {
        given: 'A deleted office'
        def office = new OfficeBuilder().build()
        office.delete(LocalDate.now(Clock.systemUTC()).minusDays(1))
        officeHolderRepo.store(office.officeBranch().owner())
        officeBranchRepo.store(office.officeBranch())
        officeRepo.store(office)

        when:
        def response = mockMvc
                .perform(get("/api/offices/${office.id()}/"))
                .andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'NOT_FOUND',
                            'error'  : 'OFFICE_NOT_FOUND',
                            'message': 'There is no office with id provided',
                    ]
            ]
        }
    }

    void "it should return ok when office is found successfully"() {
        given: 'An existent office'
        def office = new OfficeBuilder().build()
        officeHolderRepo.store(office.officeBranch().owner())
        officeBranchRepo.store(office.officeBranch())
        officeRepo.store(office)

        when:
        def response = mockMvc
                .perform(get("/api/offices/${office.id()}/"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == [
                    'id'         : office.id().toString(),
                    'name'       : office.toResponse().name,
                    'description': office.toResponse().description,
                    'capacity'   : office.toResponse().capacity,
                    'price'      : office.toResponse().price,
                    'imageUrl'   : office.toResponse().imageUrl,
                    'privacy'    : office.toResponse().privacy,
                    'deletedAt'  : null,
                    'table'      : [
                            'quantity': office.toResponse().table.quantity,
                            'capacity': office.toResponse().table.capacity,
                    ],
                    'services'   : [],
                    'equipments' : [],
            ]
        }
    }
}

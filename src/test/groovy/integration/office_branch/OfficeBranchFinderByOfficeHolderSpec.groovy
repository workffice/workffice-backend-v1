package integration.office_branch


import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderId
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.factories.OfficeBranchBuilder
import backoffice.factories.OfficeHolderBuilder
import com.fasterxml.jackson.databind.ObjectMapper
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
class OfficeBranchFinderByOfficeHolderSpec extends Specification {
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper

    void "it should return not found when there is no office holder with specified id"() {
        given: "An nonexistent office holder id"
        def officeHolderId = new OfficeHolderId()

        when:
        def response = mockMvc
                .perform(get("/api/office_holders/${officeHolderId}/office_branches/"))
                .andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()
        and:
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

    void "it should return bad request when office holder id is not uuid format"() {
        when:
        def response = mockMvc
                .perform(get("/api/office_holders/1/office_branches/"))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
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

    void "it should return all office branches related with office holder"() {
        given: "An office holder with 3 office branches"
        def officeHolder = new OfficeHolderBuilder().build()
        officeHolderRepo.store(officeHolder)
        def officeBranch1 = new OfficeBranchBuilder()
                .withOwner(officeHolder).build()
        def officeBranch2 = new OfficeBranchBuilder()
                .withOwner(officeHolder).build()
        def officeBranch3 = new OfficeBranchBuilder()
                .withOwner(officeHolder).build()
        officeBranchRepo.store(officeBranch1).get()
        officeBranchRepo.store(officeBranch2).get()
        officeBranchRepo.store(officeBranch3).get()

        when:
        def response = mockMvc
                .perform(get("/api/office_holders/${officeHolder.id()}/office_branches/"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data as Set == [
                    [
                            'id'         : officeBranch1.toResponse().id,
                            'name'       : officeBranch1.toResponse().name,
                            'description': officeBranch1.toResponse().description,
                            'phone'      : officeBranch1.toResponse().phone,
                            'created'    : officeBranch1.toResponse().created.toString(),
                            'images'     : [],
                            'location'   : [
                                    'city'    : officeBranch1.toResponse().location.city,
                                    'street'  : officeBranch1.toResponse().location.street,
                                    'province': officeBranch1.toResponse().location.province,
                                    'zipCode' : officeBranch1.toResponse().location.zipCode,
                            ]
                    ],
                    [
                            'id'         : officeBranch2.toResponse().id,
                            'name'       : officeBranch2.toResponse().name,
                            'description': officeBranch2.toResponse().description,
                            'phone'      : officeBranch2.toResponse().phone,
                            'created'    : officeBranch2.toResponse().created.toString(),
                            'images'     : [],
                            'location'   : [
                                    'city'    : officeBranch2.toResponse().location.city,
                                    'street'  : officeBranch2.toResponse().location.street,
                                    'province': officeBranch2.toResponse().location.province,
                                    'zipCode' : officeBranch2.toResponse().location.zipCode,
                            ]
                    ],
                    [
                            'id'         : officeBranch3.toResponse().id,
                            'name'       : officeBranch3.toResponse().name,
                            'description': officeBranch3.toResponse().description,
                            'phone'      : officeBranch3.toResponse().phone,
                            'created'    : officeBranch3.toResponse().created.toString(),
                            'images'     : [],
                            'location'   : [
                                    'city'    : officeBranch3.toResponse().location.city,
                                    'street'  : officeBranch3.toResponse().location.street,
                                    'province': officeBranch3.toResponse().location.province,
                                    'zipCode' : officeBranch3.toResponse().location.zipCode,
                            ]
                    ]
            ] as Set
        }
    }
}

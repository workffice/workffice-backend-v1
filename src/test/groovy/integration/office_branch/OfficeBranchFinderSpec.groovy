package integration.office_branch


import backoffice.domain.office_branch.Image
import backoffice.domain.office_branch.OfficeBranch
import backoffice.domain.office_branch.OfficeBranchId
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.factories.OfficeBranchBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.ImmutableList
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
class OfficeBranchFinderSpec extends Specification {
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper

    void "it should return bad request when office branch id is invalid"() {
        when:
        def response = mockMvc.perform(get('/api/office_branches/-1/')).andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
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

    void "it should return not found when there are no office branch with specified id"() {
        when:
        String officeBranchId = new OfficeBranchId().toString()
        def response = mockMvc.perform(get("/api/office_branches/${officeBranchId}/")).andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()
        and:
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

    void "it should return ok when office branch is returned successfully"() {
        given:
        OfficeBranch officeBranch = new OfficeBranchBuilder()
                .withImages(ImmutableList.of(new Image("someurl.com")))
                .build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)

        when:
        def response = mockMvc.perform(get("/api/office_branches/${officeBranch.id().toString()}/")).andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        verifyAll(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == [
                    'id'         : officeBranch.id().toString(),
                    'name'       : officeBranch.name(),
                    'description': officeBranch.description(),
                    'phone'      : officeBranch.phone(),
                    'created'    : officeBranch.created().toString(),
                    'images'     : [
                            ['url': 'someurl.com']
                    ],
                    'location'   : [
                            'province'  : officeBranch.location().province(),
                            'city'      : officeBranch.location().city(),
                            'street'    : officeBranch.location().street(),
                            'zipCode': officeBranch.location().zipCode()
                    ]
            ]
        }
    }
}

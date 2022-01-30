package integration.equipment

import authentication.application.dto.token.Authentication
import backoffice.domain.equipment.EquipmentRepository
import backoffice.domain.office_branch.OfficeBranchId
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolder
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.factories.EquipmentBuilder
import backoffice.factories.OfficeBranchBuilder
import backoffice.factories.OfficeHolderBuilder
import com.fasterxml.jackson.databind.ObjectMapper
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
class EquipmentsByOfficeBranchFinderSpec extends Specification {
    @Autowired
    MockMvc mockMvc
    @Autowired
    EquipmentRepository equipmentRepository
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    ObjectMapper objectMapper
    @Autowired
    AuthUtil authUtil

    void "it should return not found when there is no office branch with id specified"() {
        given: "A non existent office branch id for authenticated user"
        def officeBranchId = new OfficeBranchId()
        OfficeHolder officeHolder = new OfficeHolderBuilder().build()
        officeHolderRepo.store(officeHolder)
        Authentication authentication = authUtil.createAndLoginUser(officeHolder.email(), "1234")

        when:
        def response = mockMvc
                .perform(get("/api/office_branches/${officeBranchId}/equipments/")
                        .header("Authorization", "Bearer " + authentication.getToken()))
                .andReturn().response

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

    void "it should return bad request when office branch id is not uuid format"() {
        given:
        Authentication authentication = authUtil.createAndLoginUser("marcelo@gallardo.com", "1234")

        when:
        def response = mockMvc
                .perform(get("/api/office_branches/-1/equipments/")
                        .header("Authorization", "Bearer " + authentication.getToken()))
                .andReturn().response

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

    void "it should return equipments related with office branch"() {
        given: "A single office branch for authenticated user"
        def officeHolder = new OfficeHolderBuilder().build()
        officeHolderRepo.store(officeHolder)
        def officeBranch = new OfficeBranchBuilder().withOwner(officeHolder).build()
        officeBranchRepo.store(officeBranch)
        and: "Three equipments related with office branch"
        def equipment1 = new EquipmentBuilder().withOfficeBranch(officeBranch).build()
        def equipment2 = new EquipmentBuilder().withOfficeBranch(officeBranch).build()
        def equipment3 = new EquipmentBuilder().withOfficeBranch(officeBranch).build()
        equipmentRepository.store(equipment1)
        equipmentRepository.store(equipment2)
        equipmentRepository.store(equipment3)
        and: "Authenticated user"
        Authentication authentication = authUtil.createAndLoginUser("marcelo@gallardo.com", "1234")

        when:
        def response = mockMvc
                .perform(get("/api/office_branches/${officeBranch.id()}/equipments/")
                        .header("Authorization", "Bearer " + authentication.getToken()))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data as Set == [
                    [
                            'id'      : equipment1.id().toString(),
                            'name'    : equipment1.toResponse().name,
                            'category': equipment1.toResponse().getCategory()
                    ],
                    [
                            'id'      : equipment2.id().toString(),
                            'name'    : equipment2.toResponse().name,
                            'category': equipment2.toResponse().getCategory()
                    ],
                    [
                            'id'      : equipment3.id().toString(),
                            'name'    : equipment3.toResponse().name,
                            'category': equipment3.toResponse().getCategory()
                    ],
            ] as Set
        }
    }
}

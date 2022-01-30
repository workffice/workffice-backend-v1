package integration.equipment

import authentication.application.dto.token.Authentication
import backoffice.application.dto.equipment.EquipmentInformation
import backoffice.domain.equipment.EquipmentRepository
import backoffice.domain.office_branch.OfficeBranch
import backoffice.domain.office_branch.OfficeBranchId
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.factories.OfficeBranchBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.javafaker.Faker
import controller.AuthUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import server.WorkfficeApplication
import spock.lang.Specification

import static controller.IntegrationTestUtil.asJsonString
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class EquipmentCreatorSpec extends Specification {

    @Autowired
    MockMvc mockMvc
    @Autowired
    EquipmentRepository equipmentRepository
    @Autowired
    OfficeBranchRepository officeBranchRepository
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    AuthUtil authUtil
    @Autowired
    ObjectMapper objectMapper


    def faker = Faker.instance()

    EquipmentInformation equipmentInformationExample() {
        return EquipmentInformation.of(
                faker.name().name(),
                "TECHNOLOGY"
        )
    }

    static MockHttpServletRequestBuilder equipmentCreationRequest(
            EquipmentInformation equipmentInformation,
            String officeBranchId,
            Authentication authentication
    ) {
        return post("/api/office_branches/${officeBranchId}/equipments/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(equipmentInformation))
                .header("Authorization", "Bearer " + authentication.getToken())
    }

    void 'it should return forbidden when auth user dose not own office branch'() {
        given: 'An existent office branch'
        OfficeBranch officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepository.store(officeBranch).get()
        and: 'An authenticated user with different email than office holder'
        def authentication = authUtil.createAndLoginUser("no.auth@mail.com", "2312")

        when:
        EquipmentInformation equipmentInformation = equipmentInformationExample()
        def response = mockMvc
                .perform(equipmentCreationRequest(
                        equipmentInformation,
                        officeBranch.id().toString(),
                        authentication))
                .andReturn().response
        then:
        response.status == HttpStatus.FORBIDDEN.value()
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'OFFICE_BRANCH_FORBIDDEN',
                            'message': 'You do not have access to office branch'
                    ]
            ]
        }
    }

    void 'it should return not found when office branch does not exist'() {
        given: 'An authenticated user'
        var authentication = authUtil.createAndLoginUser("mmm@mail.com", "1234")

        when: 'User tries to create an equipment for an nonexistent office branch'
        EquipmentInformation equipmentInformation = equipmentInformationExample()
        OfficeBranchId nonExistentOfficeBranchId = new OfficeBranchId()
        def response = mockMvc
                .perform(equipmentCreationRequest(
                        equipmentInformation,
                        nonExistentOfficeBranchId.toString(),
                        authentication))
                .andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()
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

    void 'it should return created when user creates an equipment successfully'() {
        given: 'An authenticated user'
        var officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepository.store(officeBranch).get()
        var authentication = authUtil
                .createAndLoginUser(officeBranch.owner().email(), "1234")

        when:
        def equipmentInformation = EquipmentInformation.of(
                faker.name().name(),
                'CONFORT'
        )
        def response = mockMvc
                .perform(equipmentCreationRequest(
                        equipmentInformation,
                        officeBranch.id().toString(),
                        authentication))
                .andReturn().response

        then:
        response.status == HttpStatus.CREATED.value()
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data['uri'] ==~ "/api/equipments/.*/"
        }
    }
}

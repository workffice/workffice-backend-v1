package integration.office

import authentication.application.dto.token.Authentication
import backoffice.domain.equipment.EquipmentId
import backoffice.domain.equipment.EquipmentRepository
import backoffice.domain.office.OfficeId
import backoffice.domain.office.OfficeRepository
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.domain.role.Access
import backoffice.domain.role.Permission
import backoffice.domain.role.Resource
import backoffice.factories.EquipmentBuilder
import backoffice.factories.OfficeBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.javafaker.Faker
import controller.AuthUtil
import controller.CollaboratorUtil
import org.assertj.core.api.Assertions
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class OfficeEquipmentUpdaterSpec extends Specification {
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    OfficeRepository officeRepo
    @Autowired
    EquipmentRepository equipmentRepo
    @Autowired
    AuthUtil authUtil
    @Autowired
    CollaboratorUtil collaboratorUtil
    @Autowired
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper

    Faker faker = Faker.instance()

    private static MockHttpServletRequestBuilder updateOfficeEquipmentRequest(
            String id,
            Authentication token,
            Set<String> equipmentIds
    ) {
        put("/api/offices/${id}/equipments/")
                .header("Authorization", "Bearer ${token.getToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(equipmentIds))
    }

    void "it should return not authorized when token is not provided"() {
        given:
        def equipmentIds = [new EquipmentId().toString()] as Set

        when:
        def response = mockMvc.perform(put("/api/offices/1/equipments/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(equipmentIds)))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    void "it should return bad request when office id is not uuid format"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def equipmentIds = [new EquipmentId().toString()] as Set

        when:
        def response = mockMvc.perform(updateOfficeEquipmentRequest("-1", token, equipmentIds))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INVALID_OFFICE_ID',
                            'message': 'Office id provided has a wrong format',
                    ]
            ]
        }
    }

    void "it should return not found when there is no office with id provided"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def officeId = new OfficeId()
        def equipmentIds = [new EquipmentId().toString()] as Set

        when:
        def response = mockMvc.perform(updateOfficeEquipmentRequest(officeId.toString(), token, equipmentIds))
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

    void "it should return forbidden when auth user is not owner of office branch that contains office"() {
        given: 'An office that belong to an office branch'
        def office = new OfficeBuilder().build()
        officeHolderRepo.store(office.officeBranch().owner())
        officeBranchRepo.store(office.officeBranch())
        officeRepo.store(office)
        and: 'An authenticated user that is not the owner of the office branch'
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def equipmentIds = [new EquipmentId().toString()] as Set

        when:
        def response = mockMvc.perform(updateOfficeEquipmentRequest(office.id().toString(), token, equipmentIds))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'OFFICE_FORBIDDEN',
                            'message': 'Forbidden you do not have access to office',
                    ]
            ]
        }
    }

    void "it should return forbidden when collaborator does not have write access to office"() {
        given: 'An office that belong to an office branch'
        def office = new OfficeBuilder().build()
        officeHolderRepo.store(office.officeBranch().owner())
        officeBranchRepo.store(office.officeBranch())
        officeRepo.store(office)
        and: 'An authenticated collaborator that not has write access to office resource'
        def collaborator = collaboratorUtil.createCollaborator(
                office.officeBranch(),
                [Permission.create(Access.READ, Resource.OFFICE)] as Set
        )
        def token = authUtil.createAndLoginUser(collaborator.email(), "1234")
        def equipmentIds = [new EquipmentId().toString()] as Set

        when:
        def response = mockMvc.perform(updateOfficeEquipmentRequest(office.id().toString(), token, equipmentIds))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'OFFICE_FORBIDDEN',
                            'message': 'Forbidden you do not have access to office',
                    ]
            ]
        }
    }

    void "it should return bad request when there is at least one invalid equipment id"() {
        given: 'An office that belong to an office branch'
        def office = new OfficeBuilder().build()
        officeHolderRepo.store(office.officeBranch().owner())
        officeBranchRepo.store(office.officeBranch())
        officeRepo.store(office)
        and: 'An authenticated user that is not the owner of the office branch'
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def equipmentIds = ["1"] as Set

        when:
        def response = mockMvc.perform(updateOfficeEquipmentRequest(office.id().toString(), token, equipmentIds))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INVALID_EQUIPMENT_ID',
                            'message': 'There is at least one invalid equipment id',
                    ]
            ]
        }
    }

    void "it should return accepted when office is updated successfully"() {
        given: 'An office that belong to an office branch'
        def office = new OfficeBuilder().build()
        officeHolderRepo.store(office.officeBranch().owner())
        officeBranchRepo.store(office.officeBranch())
        officeRepo.store(office)
        def equipment1 = new EquipmentBuilder()
                .withOfficeBranch(office.officeBranch())
                .build()
        def equipment2 = new EquipmentBuilder()
                .withOfficeBranch(office.officeBranch())
                .build()
        equipmentRepo.store(equipment1).get()
        equipmentRepo.store(equipment2).get()
        and: 'An authenticated user that is the owner of the office branch'
        def token = authUtil.createAndLoginUser(office.officeBranch().owner().email(), "1234")
        def equipmentIds = [equipment1.id().toString(), equipment2.id().toString()] as Set

        when:
        def response = mockMvc.perform(updateOfficeEquipmentRequest(office.id().toString(), token, equipmentIds))
                .andReturn().response

        then:
        response.status == HttpStatus.ACCEPTED.value()
        and:
        def officeUpdated = officeRepo.findById(office.id()).get()
        Assertions.assertThat(officeUpdated.toResponse().getEquipments()).containsExactlyInAnyOrder(
                equipment1.toResponse(),
                equipment2.toResponse()
        )
    }

    void "it should return accepted when collaborator with right access update office successfully"() {
        given: 'An office that belong to an office branch'
        def office = new OfficeBuilder().build()
        officeHolderRepo.store(office.officeBranch().owner())
        officeBranchRepo.store(office.officeBranch())
        officeRepo.store(office)
        def equipment1 = new EquipmentBuilder()
                .withOfficeBranch(office.officeBranch())
                .build()
        def equipment2 = new EquipmentBuilder()
                .withOfficeBranch(office.officeBranch())
                .build()
        equipmentRepo.store(equipment1)
        equipmentRepo.store(equipment2)
        and: 'An authenticated collaborator with write access to office resource'
        def collaborator = collaboratorUtil.createCollaborator(
                office.officeBranch(),
                [Permission.create(Access.WRITE, Resource.OFFICE)] as Set
        )
        def token = authUtil.createAndLoginUser(collaborator.email(), "1234")
        def equipmentIds = [equipment1.id().toString()] as Set


        when:
        def response = mockMvc.perform(updateOfficeEquipmentRequest(office.id().toString(), token, equipmentIds))
                .andReturn().response

        then:
        response.status == HttpStatus.ACCEPTED.value()
        and:
        def officeUpdated = officeRepo.findById(office.id()).get()
        Assertions.assertThat(officeUpdated.toResponse().getEquipments()).containsExactlyInAnyOrder(
                equipment1.toResponse()
        )
    }
}

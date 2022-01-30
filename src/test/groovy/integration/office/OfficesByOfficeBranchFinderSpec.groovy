package integration.office

import backoffice.domain.equipment.EquipmentRepository
import backoffice.domain.office.OfficeRepository
import backoffice.domain.office_branch.OfficeBranchId
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.domain.service.ServiceRepository
import backoffice.factories.*
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
class OfficesByOfficeBranchFinderSpec extends Specification {
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    OfficeRepository officeRepo
    @Autowired
    ServiceRepository serviceRepo
    @Autowired
    EquipmentRepository equipmentRepo
    @Autowired
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper

    void "it should return not found when there is no office branch with id specified"() {
        given: "A non existent office branch id"
        def officeBranchId = new OfficeBranchId()

        when:
        def response = mockMvc
                .perform(get("/api/office_branches/${officeBranchId}/offices/"))
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
        when:
        def response = mockMvc
                .perform(get("/api/office_branches/-1/offices/"))
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

    void "it should return offices related with office branch"() {
        given: "A single office branch"
        def officeHolder = new OfficeHolderBuilder().build()
        officeHolderRepo.store(officeHolder)
        def officeBranch = new OfficeBranchBuilder().withOwner(officeHolder).build()
        officeBranchRepo.store(officeBranch)
        def service1 = new ServiceBuilder()
                .withOfficeBranch(officeBranch)
                .build()
        def service2 = new ServiceBuilder()
                .withOfficeBranch(officeBranch)
                .build()
        def equipment1 = new EquipmentBuilder()
                .withOfficeBranch(officeBranch)
                .build()
        def equipment2 = new EquipmentBuilder()
                .withOfficeBranch(officeBranch)
                .build()
        serviceRepo.store(service1)
        serviceRepo.store(service2)
        equipmentRepo.store(equipment1)
        equipmentRepo.store(equipment2)
        and: "Three offices related with office branch"
        def office1 = new OfficeBuilder().withOfficeBranch(officeBranch).build()
        office1.addServices([service1] as Set)
        def office2 = new OfficeBuilder().withOfficeBranch(officeBranch).build()
        office2.addServices([service2] as Set)
        office2.addEquipments([equipment1] as Set)
        def office3 = new OfficeBuilder().withOfficeBranch(officeBranch).build()
        office3.addServices([] as Set)
        office3.addEquipments([equipment2] as Set)
        officeRepo.store(office1)
        officeRepo.store(office2)
        officeRepo.store(office3)

        when:
        def response = mockMvc
                .perform(get("/api/office_branches/${officeBranch.id()}/offices/"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data as Set == [
                    [
                            'id'         : office1.id().toString(),
                            'name'       : office1.toResponse().name,
                            'description': office1.toResponse().description,
                            'capacity'   : office1.toResponse().capacity,
                            'price'      : office1.toResponse().price,
                            'imageUrl'   : office1.toResponse().imageUrl,
                            'privacy'    : office1.toResponse().privacy,
                            'deletedAt'  : null,
                            'table'      : [
                                    'quantity': office1.toResponse().table.quantity,
                                    'capacity': office1.toResponse().table.capacity
                            ],
                            'services'   : [
                                    [
                                            'id'      : service1.toResponse().id,
                                            'name'    : service1.toResponse().name,
                                            'category': service1.toResponse().category,
                                    ],
                            ],
                            'equipments' : [],
                    ],
                    [
                            'id'         : office2.id().toString(),
                            'name'       : office2.toResponse().name,
                            'description': office2.toResponse().description,
                            'capacity'   : office2.toResponse().capacity,
                            'price'      : office2.toResponse().price,
                            'imageUrl'   : office2.toResponse().imageUrl,
                            'privacy'    : office2.toResponse().privacy,
                            'deletedAt'  : null,
                            'table'      : [
                                    'quantity': office2.toResponse().table.quantity,
                                    'capacity': office2.toResponse().table.capacity
                            ],
                            'services'   : [
                                    [
                                            'id'      : service2.toResponse().id,
                                            'name'    : service2.toResponse().name,
                                            'category': service2.toResponse().category,
                                    ]
                            ],
                            'equipments' : [
                                    [
                                            'id'      : equipment1.toResponse().id,
                                            'name'    : equipment1.toResponse().name,
                                            'category': equipment1.toResponse().category,
                                    ],
                            ],
                    ],
                    [
                            'id'         : office3.id().toString(),
                            'name'       : office3.toResponse().name,
                            'description': office3.toResponse().description,
                            'capacity'   : office3.toResponse().capacity,
                            'price'      : office3.toResponse().price,
                            'imageUrl'   : office3.toResponse().imageUrl,
                            'privacy'    : office3.toResponse().privacy,
                            'deletedAt'  : null,
                            'table'      : [
                                    'quantity': office3.toResponse().table.quantity,
                                    'capacity': office3.toResponse().table.capacity
                            ],
                            'services'   : [],
                            'equipments' : [
                                    [
                                            'id'      : equipment2.toResponse().id,
                                            'name'    : equipment2.toResponse().name,
                                            'category': equipment2.toResponse().category,
                                    ]
                            ],
                    ],
            ] as Set
        }
    }

    void "it should return offices related with office branch that are not deleted yet"() {
        given: "A single office branch"
        def officeHolder = new OfficeHolderBuilder().build()
        officeHolderRepo.store(officeHolder)
        def officeBranch = new OfficeBranchBuilder().withOwner(officeHolder).build()
        officeBranchRepo.store(officeBranch)
        and: "Three offices related with office branch"
        def office1 = new OfficeBuilder().withOfficeBranch(officeBranch).build()
        office1.delete(LocalDate.now(Clock.systemUTC()).plusDays(10))
        def office2 = new OfficeBuilder().withOfficeBranch(officeBranch).build()
        office2.delete(LocalDate.now(Clock.systemUTC()).minusDays(1))
        def office3 = new OfficeBuilder().withOfficeBranch(officeBranch).build()
        office3.delete(LocalDate.now(Clock.systemUTC()).minusDays(6))
        officeRepo.store(office1)
        officeRepo.store(office2)
        officeRepo.store(office3)

        when:
        def response = mockMvc
                .perform(get("/api/office_branches/${officeBranch.id()}/offices/"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and: 'Office1 is returned because it will be deleted in 10 days while office2 and office3 are already deleted'
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data as Set == [
                    [
                            'id'         : office1.id().toString(),
                            'name'       : office1.toResponse().name,
                            'description': office1.toResponse().description,
                            'capacity'   : office1.toResponse().capacity,
                            'price'      : office1.toResponse().price,
                            'imageUrl'   : office1.toResponse().imageUrl,
                            'privacy'    : office1.toResponse().privacy,
                            'deletedAt'  : office1.toResponse().deletedAt.toString(),
                            'table'      : [
                                    'quantity': office1.toResponse().table.quantity,
                                    'capacity': office1.toResponse().table.capacity
                            ],
                            'services'   : [],
                            'equipments' : [],
                    ]
            ] as Set
        }
    }
}

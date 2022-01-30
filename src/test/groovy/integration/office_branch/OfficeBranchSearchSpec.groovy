package integration.office_branch

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import search.domain.OfficeBranchRepository
import search.domain.OfficePrivacy
import search.factories.OfficeBranchBuilder
import search.factories.OfficeBuilder
import server.WorkfficeApplication
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class OfficeBranchSearchSpec extends Specification {
    @Autowired
    MongoTemplate mongoTemplate
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper

    def cleanup() {
        var collection = mongoTemplate.getCollection("office_branches")
        collection.drop()
    }

    void "it should return empty when there is no office branch stored"() {
        when:
        def response = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/office_branches/search/?name=unexistent"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == []
            it.pagination == [
                    'pageSize'   : 20,
                    'totalPages' : 0,
                    'currentPage': 0,
                    'lastPage'   : true
            ]
        }
    }

    void "it should return office branches that match the name specified"() {
        given:
        def officeBranch1 = OfficeBranchBuilder.builder()
                .withName("Uen")
                .build()
        def officeBranch2 = OfficeBranchBuilder.builder()
                .withName("Fuente")
                .build()
        def officeBranch3 = OfficeBranchBuilder.builder()
                .withName("Puente")
                .build()
        officeBranchRepo.store(officeBranch1)
        officeBranchRepo.store(officeBranch2)
        officeBranchRepo.store(officeBranch3)

        when:
        def response = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/office_branches/search/?name=uen"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data.size() == 1
            it.data == [
                    [
                            'id'      : officeBranch1.toResponse().id,
                            'name'    : officeBranch1.toResponse().name,
                            'phone'   : officeBranch1.toResponse().phone,
                            'province': officeBranch1.toResponse().province,
                            'city'    : officeBranch1.toResponse().city,
                            'street'  : officeBranch1.toResponse().street,
                            'offices' : [],
                            'images'  : []
                    ]
            ]
            it.pagination == [
                    'pageSize'   : 20,
                    'totalPages' : 1,
                    'currentPage': 1,
                    'lastPage'   : true,
            ]
        }
    }

    void "it should return the office branches that match the criteria specified"() {
        given:
        def officeBranch1 = OfficeBranchBuilder.builder()
                .withName("Puente")
                .build()
        def officeBranch2 = OfficeBranchBuilder.builder()
                .withName("Puente")
                .build()
        def office = OfficeBuilder.builder()
                .withPrivacy(OfficePrivacy.PRIVATE).build()
        def officeBranch3 = OfficeBranchBuilder.builder()
                .withName("Puente")
                .withImages(["image1.url", "image2.url"])
                .addOffice(office)
                .build()
        officeBranchRepo.store(officeBranch1)
        officeBranchRepo.store(officeBranch2)
        officeBranchRepo.store(officeBranch3)

        when:
        def response = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/office_branches/search/?name=PuEnTe&office_type=PRIVATE"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == [
                    [
                            'id'      : officeBranch3.toResponse().id,
                            'name'    : officeBranch3.toResponse().name,
                            'phone'   : officeBranch3.toResponse().phone,
                            'province': officeBranch3.toResponse().province,
                            'city'    : officeBranch3.toResponse().city,
                            'street'  : officeBranch3.toResponse().street,
                            'offices' : [
                                    [
                                            id              : officeBranch3.toResponse().offices[0].id,
                                            name            : officeBranch3.toResponse().offices[0].name,
                                            price           : officeBranch3.toResponse().offices[0].price,
                                            capacity        : officeBranch3.toResponse().offices[0].capacity,
                                            tablesQuantity  : officeBranch3.toResponse().offices[0].tablesQuantity,
                                            capacityPerTable: officeBranch3.toResponse().offices[0].capacityPerTable,
                                            privacy         : officeBranch3.toResponse().offices[0].privacy,
                                    ]
                            ],
                            'images'  : ['image1.url', 'image2.url']
                    ]
            ]
            it.pagination == [
                    'pageSize'   : 20,
                    'totalPages' : 1,
                    'currentPage': 1,
                    'lastPage'   : true,
            ]
        }
    }

    void "it should return the first 2 office branches and return pagination information"() {
        given:
        def officeBranch1 = OfficeBranchBuilder.builder()
                .withName("Puente")
                .build()
        def officeBranch2 = OfficeBranchBuilder.builder()
                .withName("Puente")
                .build()
        def officeBranch3 = OfficeBranchBuilder.builder()
                .withName("Puente")
                .build()
        def officeBranch4 = OfficeBranchBuilder.builder()
                .withName("Puente")
                .build()
        def officeBranch5 = OfficeBranchBuilder.builder()
                .withName("Puente")
                .build()
        officeBranchRepo.store(officeBranch1)
        officeBranchRepo.store(officeBranch2)
        officeBranchRepo.store(officeBranch3)
        officeBranchRepo.store(officeBranch4)
        officeBranchRepo.store(officeBranch5)

        when:
        def response = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/office_branches/search/?name=Puente&size=2"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data.size() == 2
            it.pagination == [
                    'pageSize'   : 2,
                    'totalPages' : 3,
                    'currentPage': 1,
                    'lastPage'   : false,
            ]
        }
    }

    void "it should return office branches that contains at least one office with capacity greater than 40"() {
        given:
        def office1 = OfficeBuilder.builder().withCapacity(50).build()
        def officeBranch1 = OfficeBranchBuilder.builder()
                .addOffice(office1)
                .build()
        def office2 = OfficeBuilder.builder().withCapacity(10).build()
        def officeBranch2 = OfficeBranchBuilder.builder()
                .addOffice(office2)
                .build()
        def office3 = OfficeBuilder.builder().withCapacity(30).build()
        def officeBranch3 = OfficeBranchBuilder.builder()
                .addOffice(office3)
                .build()
        def office4 = OfficeBuilder.builder().withCapacity(40).build()
        def officeBranch4 = OfficeBranchBuilder.builder()
                .addOffice(office4)
                .build()
        def office5 = OfficeBuilder.builder().withCapacity(5).build()
        def officeBranch5 = OfficeBranchBuilder.builder()
                .addOffice(office5)
                .build()
        officeBranchRepo.store(officeBranch1)
        officeBranchRepo.store(officeBranch2)
        officeBranchRepo.store(officeBranch3)
        officeBranchRepo.store(officeBranch4)
        officeBranchRepo.store(officeBranch5)

        when:
        def response = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/office_branches/search/?office_capacity_gt=40"))
                .andReturn().response

        then:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data.size() == 2
            it.data as Set == [
                    [
                            'id'      : officeBranch1.toResponse().id,
                            'name'    : officeBranch1.toResponse().name,
                            'phone'   : officeBranch1.toResponse().phone,
                            'province': officeBranch1.toResponse().province,
                            'city'    : officeBranch1.toResponse().city,
                            'street'  : officeBranch1.toResponse().street,
                            'offices' : [
                                    [
                                            id              : officeBranch1.toResponse().offices[0].id,
                                            name            : officeBranch1.toResponse().offices[0].name,
                                            price           : officeBranch1.toResponse().offices[0].price,
                                            capacity        : officeBranch1.toResponse().offices[0].capacity,
                                            tablesQuantity  : officeBranch1.toResponse().offices[0].tablesQuantity,
                                            capacityPerTable: officeBranch1.toResponse().offices[0].capacityPerTable,
                                            privacy         : officeBranch1.toResponse().offices[0].privacy,
                                    ]
                            ],
                            'images'  : []
                    ],
                    [
                            'id'      : officeBranch4.toResponse().id,
                            'name'    : officeBranch4.toResponse().name,
                            'phone'   : officeBranch4.toResponse().phone,
                            'province': officeBranch4.toResponse().province,
                            'city'    : officeBranch4.toResponse().city,
                            'street'  : officeBranch4.toResponse().street,
                            'offices' : [
                                    [
                                            id              : officeBranch4.toResponse().offices[0].id,
                                            name            : officeBranch4.toResponse().offices[0].name,
                                            price           : officeBranch4.toResponse().offices[0].price,
                                            capacity        : officeBranch4.toResponse().offices[0].capacity,
                                            tablesQuantity  : officeBranch4.toResponse().offices[0].tablesQuantity,
                                            capacityPerTable: officeBranch4.toResponse().offices[0].capacityPerTable,
                                            privacy         : officeBranch4.toResponse().offices[0].privacy,
                                    ]
                            ],
                            'images'  : []
                    ]
            ] as Set
        }
    }

    void "it should return office branches that contains at least one office with capacity less than 15"() {
        given:
        def office1 = OfficeBuilder.builder().withCapacity(50).build()
        def officeBranch1 = OfficeBranchBuilder.builder()
                .addOffice(office1)
                .build()
        def office2 = OfficeBuilder.builder().withCapacity(10).build()
        def officeBranch2 = OfficeBranchBuilder.builder()
                .addOffice(office2)
                .build()
        def office3 = OfficeBuilder.builder().withCapacity(30).build()
        def officeBranch3 = OfficeBranchBuilder.builder()
                .addOffice(office3)
                .build()
        def office4 = OfficeBuilder.builder().withCapacity(40).build()
        def officeBranch4 = OfficeBranchBuilder.builder()
                .addOffice(office4)
                .build()
        def office5 = OfficeBuilder.builder().withCapacity(5).build()
        def officeBranch5 = OfficeBranchBuilder.builder()
                .addOffice(office5)
                .build()
        officeBranchRepo.store(officeBranch1)
        officeBranchRepo.store(officeBranch2)
        officeBranchRepo.store(officeBranch3)
        officeBranchRepo.store(officeBranch4)
        officeBranchRepo.store(officeBranch5)

        when:
        def response = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/office_branches/search/?office_capacity_lt=15"))
                .andReturn().response

        then:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data.size() == 2
            it.data as Set == [
                    [
                            'id'      : officeBranch2.toResponse().id,
                            'name'    : officeBranch2.toResponse().name,
                            'phone'   : officeBranch2.toResponse().phone,
                            'province': officeBranch2.toResponse().province,
                            'city'    : officeBranch2.toResponse().city,
                            'street'  : officeBranch2.toResponse().street,
                            'offices' : [
                                    [
                                            id              : officeBranch2.toResponse().offices[0].id,
                                            name            : officeBranch2.toResponse().offices[0].name,
                                            price           : officeBranch2.toResponse().offices[0].price,
                                            capacity        : officeBranch2.toResponse().offices[0].capacity,
                                            tablesQuantity  : officeBranch2.toResponse().offices[0].tablesQuantity,
                                            capacityPerTable: officeBranch2.toResponse().offices[0].capacityPerTable,
                                            privacy         : officeBranch2.toResponse().offices[0].privacy,
                                    ]
                            ],
                            'images'  : []
                    ],
                    [
                            'id'      : officeBranch5.toResponse().id,
                            'name'    : officeBranch5.toResponse().name,
                            'phone'   : officeBranch5.toResponse().phone,
                            'province': officeBranch5.toResponse().province,
                            'city'    : officeBranch5.toResponse().city,
                            'street'  : officeBranch5.toResponse().street,
                            'offices' : [
                                    [
                                            id              : officeBranch5.toResponse().offices[0].id,
                                            name            : officeBranch5.toResponse().offices[0].name,
                                            price           : officeBranch5.toResponse().offices[0].price,
                                            capacity        : officeBranch5.toResponse().offices[0].capacity,
                                            tablesQuantity  : officeBranch5.toResponse().offices[0].tablesQuantity,
                                            capacityPerTable: officeBranch5.toResponse().offices[0].capacityPerTable,
                                            privacy         : officeBranch5.toResponse().offices[0].privacy,
                                    ]
                            ],
                            'images'  : []
                    ]
            ] as Set
        }
    }

    void "it should return office branches that contains at least one office with capacity between 20 and 30"() {
        given:
        def office1 = OfficeBuilder.builder().withCapacity(50).build()
        def officeBranch1 = OfficeBranchBuilder.builder()
                .addOffice(office1)
                .build()
        def office2 = OfficeBuilder.builder().withCapacity(10).build()
        def officeBranch2 = OfficeBranchBuilder.builder()
                .addOffice(office2)
                .build()
        def office3 = OfficeBuilder.builder().withCapacity(25).build()
        def officeBranch3 = OfficeBranchBuilder.builder()
                .addOffice(office3)
                .build()
        def office4 = OfficeBuilder.builder().withCapacity(40).build()
        def officeBranch4 = OfficeBranchBuilder.builder()
                .addOffice(office4)
                .build()
        def office5 = OfficeBuilder.builder().withCapacity(5).build()
        def officeBranch5 = OfficeBranchBuilder.builder()
                .addOffice(office5)
                .build()
        officeBranchRepo.store(officeBranch1)
        officeBranchRepo.store(officeBranch2)
        officeBranchRepo.store(officeBranch3)
        officeBranchRepo.store(officeBranch4)
        officeBranchRepo.store(officeBranch5)

        when:
        def response = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/office_branches/search/?office_capacity_gt=20&office_capacity_lt=30"))
                .andReturn().response

        then:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data.size() == 1
            it.data as Set == [
                    [
                            'id'      : officeBranch3.toResponse().id,
                            'name'    : officeBranch3.toResponse().name,
                            'phone'   : officeBranch3.toResponse().phone,
                            'province': officeBranch3.toResponse().province,
                            'city'    : officeBranch3.toResponse().city,
                            'street'  : officeBranch3.toResponse().street,
                            'offices' : [
                                    [
                                            id              : officeBranch3.toResponse().offices[0].id,
                                            name            : officeBranch3.toResponse().offices[0].name,
                                            price           : officeBranch3.toResponse().offices[0].price,
                                            capacity        : officeBranch3.toResponse().offices[0].capacity,
                                            tablesQuantity  : officeBranch3.toResponse().offices[0].tablesQuantity,
                                            capacityPerTable: officeBranch3.toResponse().offices[0].capacityPerTable,
                                            privacy         : officeBranch3.toResponse().offices[0].privacy,
                                    ]
                            ],
                            'images'  : []
                    ],
            ] as Set
        }
    }

    void "it should return office branch that fulfills all the requested conditions"() {
        given: 'Office branches that fulfill some conditions but not all of them'
        def office1 = OfficeBuilder.builder().withCapacity(50).build()
        def officeBranch1 = OfficeBranchBuilder.builder()
                .addOffice(office1)
                .build()
        def office2 = OfficeBuilder.builder().withCapacity(25).build()
        def officeBranch2 = OfficeBranchBuilder.builder()
                .addOffice(office2)
                .withName("M")
                .build()
        def office4 = OfficeBuilder.builder().withCapacity(40).build()
        def officeBranch4 = OfficeBranchBuilder.builder()
                .addOffice(office4)
                .withName("Puente")
                .build()
        def office5 = OfficeBuilder.builder()
                .withCapacity(22)
                .withPrivacy(OfficePrivacy.SHARED)
                .build()
        def officeBranch5 = OfficeBranchBuilder.builder()
                .addOffice(office5)
                .withName("Puente")
                .build()
        officeBranchRepo.store(officeBranch1)
        officeBranchRepo.store(officeBranch2)
        officeBranchRepo.store(officeBranch4)
        officeBranchRepo.store(officeBranch5)
        and: 'An office branch that fulfill all the conditions specified'
        def office3 = OfficeBuilder.builder()
                .withCapacity(25)
                .withPrivacy(OfficePrivacy.PRIVATE)
                .build()
        def officeBranch3 = OfficeBranchBuilder.builder()
                .addOffice(office3)
                .withName("Puente")
                .build()
        officeBranchRepo.store(officeBranch3)

        when:
        def response = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/office_branches/search/?name=Puente&office_type=PRIVATE&office_capacity_gt=20&office_capacity_lt=30"))
                .andReturn().response

        then:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data.size() == 1
            it.data as Set == [
                    [
                            'id'      : officeBranch3.toResponse().id,
                            'name'    : officeBranch3.toResponse().name,
                            'phone'   : officeBranch3.toResponse().phone,
                            'province': officeBranch3.toResponse().province,
                            'city'    : officeBranch3.toResponse().city,
                            'street'  : officeBranch3.toResponse().street,
                            'offices' : [
                                    [
                                            id              : officeBranch3.toResponse().offices[0].id,
                                            name            : officeBranch3.toResponse().offices[0].name,
                                            price           : officeBranch3.toResponse().offices[0].price,
                                            capacity        : officeBranch3.toResponse().offices[0].capacity,
                                            tablesQuantity  : officeBranch3.toResponse().offices[0].tablesQuantity,
                                            capacityPerTable: officeBranch3.toResponse().offices[0].capacityPerTable,
                                            privacy         : officeBranch3.toResponse().offices[0].privacy,
                                    ]
                            ],
                            'images'  : []
                    ],
            ] as Set
        }
    }
}

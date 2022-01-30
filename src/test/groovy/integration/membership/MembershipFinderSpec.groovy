package integration.membership


import backoffice.domain.membership.MembershipRepository
import backoffice.domain.office_branch.OfficeBranchId
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.factories.MembershipBuilder
import backoffice.factories.OfficeBranchBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.javafaker.Faker
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
class MembershipFinderSpec extends Specification {
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    MembershipRepository membershipRepo
    @Autowired
    AuthUtil authUtil
    @Autowired
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper

    Faker faker = Faker.instance()

    void "it should return bad request when office branch id is not uuid format"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(get("/api/office_branches/1/memberships/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INVALID_OFFICE_BRANCH_ID',
                            'message': 'The office branch id provided is invalid',
                    ]
            ]
        }
    }

    void "it should return not found when office branch does not exist"() {
        given:
        def officeBranchId = new OfficeBranchId()
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc.perform(get("/api/office_branches/${officeBranchId}/memberships/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'NOT_FOUND',
                            'error'  : 'OFFICE_BRANCH_NOT_FOUND',
                            'message': 'The office branch requested does not exist',
                    ]
            ]
        }
    }

    void "it should return ok with memberships related to office branch"() {
        given:
        def officeBranch = new OfficeBranchBuilder().build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        def membership1 = new MembershipBuilder()
                .withOfficeBranch(officeBranch)
                .build()
        def membership2 = new MembershipBuilder()
                .withOfficeBranch(officeBranch)
                .build()
        def membership3 = new MembershipBuilder()
                .withOfficeBranch(officeBranch)
                .build()
        membershipRepo.store(membership1)
        membershipRepo.store(membership2)
        membershipRepo.store(membership3)
        and:
        def token = authUtil.createAndLoginUser(
                officeBranch.owner().email(),
                "1234"
        )

        when:
        def response = mockMvc.perform(get("/api/office_branches/${officeBranch.id()}/memberships/")
                .header("Authorization", "Bearer ${token.getToken()}"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data as Set == [
                    [
                            'id'           : membership1.toResponse().id,
                            'name'         : membership1.toResponse().name,
                            'description'  : membership1.toResponse().description,
                            'accessDays'   : [],
                            'pricePerMonth': membership1.toResponse().pricePerMonth,
                    ],
                    [
                            'id'           : membership2.toResponse().id,
                            'name'         : membership2.toResponse().name,
                            'description'  : membership2.toResponse().description,
                            'accessDays'   : [],
                            'pricePerMonth': membership2.toResponse().pricePerMonth,
                    ],
                    [
                            'id'           : membership3.toResponse().id,
                            'name'         : membership3.toResponse().name,
                            'description'  : membership3.toResponse().description,
                            'accessDays'   : [],
                            'pricePerMonth': membership3.toResponse().pricePerMonth,
                    ],
            ] as Set
        }
    }
}

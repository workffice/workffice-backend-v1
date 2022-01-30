package integration.auth_user

import authentication.application.dto.token.Authentication
import authentication.domain.user.AuthUserRepository
import backoffice.domain.collaborator.CollaboratorRepository
import backoffice.domain.collaborator.Status
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.factories.CollaboratorBuilder
import backoffice.factories.OfficeHolderBuilder
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
class UserFinderSpec extends Specification {
    @Autowired
    AuthUserRepository authUserRepo
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    CollaboratorRepository collaboratorRepo
    @Autowired
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper
    @Autowired
    AuthUtil authUtil

    Faker faker = Faker.instance()

    void "it should return ok with user information with user type renter"() {
        given:
        def email = faker.internet().emailAddress()
        Authentication authentication = authUtil.createAndLoginUser(email, "1234")

        when:
        def response = mockMvc.perform(
                get("/api/users/me/")
                        .header("Authorization", "Bearer ${authentication.getToken()}")
        ).andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        def authUser = authUserRepo.findByEmail(email).get()
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == [
                    'id'          : authUser.toResponse().id,
                    'email'       : authUser.toResponse().email,
                    'name'        : authUser.toResponse().name,
                    'lastname'    : authUser.toResponse().lastname,
                    'address'     : authUser.toResponse().address,
                    'bio'         : authUser.toResponse().bio,
                    'userType'    : "RENTER",
                    'profileImage': authUser.toResponse().profileImage
            ]
        }
    }

    void "it should return ok with user information with user type collaborator"() {
        given:
        def email = faker.internet().emailAddress()
        Authentication authentication = authUtil.createAndLoginUser(email, "1234")
        def collaborator = new CollaboratorBuilder()
                .withEmail(email)
                .withStatus(Status.ACTIVE)
                .build()
        officeHolderRepo.store(collaborator.officeBranch().owner())
        officeBranchRepo.store(collaborator.officeBranch())
        collaboratorRepo.store(collaborator)

        when:
        def response = mockMvc.perform(
                get("/api/users/me/")
                        .header("Authorization", "Bearer ${authentication.getToken()}")
        ).andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        def authUser = authUserRepo.findByEmail(email).get()
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == [
                    'id'          : authUser.toResponse().id,
                    'email'       : authUser.toResponse().email,
                    'name'        : authUser.toResponse().name,
                    'lastname'    : authUser.toResponse().lastname,
                    'address'     : authUser.toResponse().address,
                    'bio'         : authUser.toResponse().bio,
                    'userType'    : "COLLABORATOR",
                    'profileImage': authUser.toResponse().profileImage
            ]
        }
    }

    void "it should return ok with user information with user type office holder"() {
        given:
        def email = faker.internet().emailAddress()
        Authentication authentication = authUtil.createAndLoginUser(email, "1234")
        def officeHolder = new OfficeHolderBuilder().withEmail(email).build()
        officeHolderRepo.store(officeHolder)

        when:
        def response = mockMvc.perform(
                get("/api/users/me/")
                        .header("Authorization", "Bearer ${authentication.getToken()}")
        ).andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        def authUser = authUserRepo.findByEmail(email).get()
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == [
                    'id'          : authUser.toResponse().id,
                    'email'       : authUser.toResponse().email,
                    'name'        : authUser.toResponse().name,
                    'lastname'    : authUser.toResponse().lastname,
                    'address'     : authUser.toResponse().address,
                    'bio'         : authUser.toResponse().bio,
                    'userType'    : "OFFICE_HOLDER",
                    'profileImage': authUser.toResponse().profileImage
            ]
        }
    }

    void "it should return unauthorized when access to user information without a token"() {
        when:
        def response = mockMvc.perform(get("/api/users/me/")).andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }
}

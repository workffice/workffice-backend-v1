package integration.auth_user

import authentication.application.dto.token.Authentication
import authentication.application.dto.user.UserUpdateInformation
import authentication.domain.user.AuthUserId
import authentication.domain.user.AuthUserRepository
import authentication.domain.user.PasswordEncoder
import authentication.factories.AuthUserBuilder
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class AuthUserUpdateSpec extends Specification {
    @Autowired
    AuthUserRepository authUserRepo
    @Autowired
    PasswordEncoder passwordEncoder
    @Autowired
    AuthUtil authUtil
    @Autowired
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper
    Faker faker = Faker.instance()

    void "it should return not authorized when token is not provided"() {
        given:
        def userUpdateInformation = UserUpdateInformation.of(
                "New name",
                "New lastname",
                "New Address",
                "Some bio",
                "imageurl.com"
        )

        when:
        def response = mockMvc.perform(put("/api/users/1/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userUpdateInformation)))
                .andReturn().response

        then:
        response.status == HttpStatus.UNAUTHORIZED.value()
    }

    private static MockHttpServletRequestBuilder updateUserRequest(
            String nonExistentId,
            Authentication token,
            UserUpdateInformation userUpdateInformation
    ) {
        put("/api/users/${nonExistentId}/")
                .header("Authorization", "Bearer ${token.getToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userUpdateInformation))
    }

    void "it should return bad request when id provided is invalid"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def userUpdateInformation = UserUpdateInformation.of(
                "New name",
                "New lastname",
                "New Address",
                "Some bio",
                "imageurl.com"
        )

        when:
        def response = mockMvc
                .perform(updateUserRequest("-1", token, userUpdateInformation))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INVALID_USER_ID',
                            'message': 'The user id provided is invalid',
                    ]
            ]
        }
    }

    void "it should return not found when there is no auth user with id provided"() {
        given:
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")
        def userUpdateInformation = UserUpdateInformation.of(
                "New name",
                "New lastname",
                "New Address",
                "Some bio",
                "imageurl.com"
        )
        def nonExistentId = new AuthUserId()

        when:
        def response = mockMvc
                .perform(updateUserRequest(nonExistentId.toString(), token, userUpdateInformation))
                .andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'NOT_FOUND',
                            'error'  : 'USER_NOT_FOUND',
                            'message': 'There is no user with email requested',
                    ]
            ]
        }
    }

    void "it should return forbidden when auth user is not the same that tries to modify"() {
        given: 'An existent user'
        def userUpdateInformation = UserUpdateInformation.of(
                "New name",
                "New lastname",
                "New Address",
                "Some bio",
                "imageurl.com"
        )
        def user = new AuthUserBuilder().build()
        authUserRepo.store(user)
        and: 'An authenticated user that is not the same that wants to modify'
        def token = authUtil.createAndLoginUser(faker.internet().emailAddress(), "1234")

        when:
        def response = mockMvc
                .perform(updateUserRequest(user.id().toString(), token, userUpdateInformation))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'USER_FORBIDDEN',
                            'message': 'You don\'t have access to this auth user',
                    ]
            ]
        }
    }

    void "it should return ok when successfully update user information"() {
        given:
        def user = new AuthUserBuilder()
                .withPassword(passwordEncoder.encode("1234"))
                .build()
        authUserRepo.store(user)
        def token = authUtil.loginUser(user.email(), "1234")
        def userUpdateInformation = UserUpdateInformation.of(
                "New name",
                "New lastname",
                "New Address",
                "Some bio",
                "imageurl.com"
        )

        when:
        def response = mockMvc
                .perform(updateUserRequest(user.id().toString(), token, userUpdateInformation))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        def userUpdated = authUserRepo.findById(user.id()).get()
        userUpdated.toResponse().name == "New name"
        userUpdated.toResponse().lastname == "New lastname"
        userUpdated.toResponse().address == "New Address"
        userUpdated.toResponse().bio == "Some bio"
        userUpdated.toResponse().profileImage == "imageurl.com"
    }
}

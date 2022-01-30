package integration.auth_user

import authentication.application.dto.user.UserInformation
import authentication.domain.user.AuthUser
import authentication.domain.user.AuthUserRepository
import authentication.factories.AuthUserBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.Sets
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import server.WorkfficeApplication
import shared.domain.email.EmailSender
import spock.lang.Shared
import spock.lang.Specification

import static controller.IntegrationTestUtil.asJsonString
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class UserRegisterSpec extends Specification {

    @Autowired
    protected MockMvc mockMvc
    @MockBean
    EmailSender emailSender
    @Autowired
    AuthUserRepository authUserRepository
    @Autowired
    ObjectMapper objectMapper

    @Shared
    errorInvalidEmail = ['code': 'INVALID', 'error': 'INVALID_EMAIL', 'message': 'Email format is invalid']
    @Shared
    errorMissingEmail = ['code': 'INVALID', 'error': 'INVALID_EMAIL', 'message': 'Email is required']
    @Shared
    errorMissingPassword = ['code': 'INVALID', 'error': 'INVALID_PASSWORD', 'message': 'Password is required']
    @Shared
    errorMissingUserType = ['code': 'INVALID', 'error': 'INVALID_TYPE', 'message': 'User type is required']

    void "it should return bad request when input data is invalid or missing"() {
        given:
        UserInformation information = new UserInformation(email, password, userType)

        when:
        def response = mockMvc.perform(post("/api/users/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(information)))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            Sets.newHashSet(it.errors) == Sets.newHashSet(expectedErrors)
        }

        where:
        email              | password | userType        || expectedErrors
        "invalidemail.com" | "13"     | "OFFICE_HOLDER" || [errorInvalidEmail]
        ""                 | ""       | ""              || [errorMissingEmail, errorMissingPassword, errorMissingUserType]
        "invalidemail.com" | ""       | "OFFICE_HOLDER" || [errorInvalidEmail, errorMissingPassword]
    }

    void "it should return created when create a new user successfully"() {
        given:
        UserInformation information = new UserInformation("test@mail.com", "1234", "OFFICE_HOLDER")

        when:
        def response = mockMvc.perform(
                post("/api/users/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(information))
        ).andReturn().response

        then:
        response.status == HttpStatus.CREATED.value()
        and:
        AuthUser authUser = authUserRepository.findByEmail("test@mail.com").get()
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == [
                    'uri': "/api/users/${authUser.id()}/"
            ]
        }
    }

    void "it should return bad request when there is an existent user with same email provided"() {
        given: "An existent user with test2@mail.com email"
        AuthUser authUser = new AuthUserBuilder()
                .withEmail("test2@mail.com")
                .build()
        authUserRepository.store(authUser)
        UserInformation information = new UserInformation("test2@mail.com", "12", "OFFICE_HOLDER")

        when:
        def response = mockMvc.perform(post("/api/users/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(information)))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'USER_ALREADY_EXISTS',
                            'message': 'There is another user with the same email'
                    ]
            ]
        }
    }
}

package integration.auth_user

import authentication.application.dto.user.UserLoginInformation
import authentication.domain.token.Token
import authentication.domain.token.TokenGenerator
import authentication.domain.token.TokenRepository
import authentication.domain.user.AuthUser
import authentication.domain.user.AuthUserRepository
import authentication.domain.user.PasswordEncoder
import authentication.domain.user.Status
import authentication.factories.AuthUserBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import server.WorkfficeApplication
import spock.lang.Specification

import static controller.IntegrationTestUtil.asJsonString
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class UserAuthenticationSpec extends Specification {
    @Autowired
    MockMvc mockMvc
    @Autowired
    AuthUserRepository authUserRepository
    @Autowired
    TokenRepository tokenRepository
    @Autowired
    PasswordEncoder passwordEncoder
    @MockBean
    TokenGenerator tokenGenerator
    @Autowired
    ObjectMapper objectMapper

    void "it should return ok when user login successfully"() {
        given: "A user with email test3@mail.com and password 1234"
        AuthUser authUser = new AuthUserBuilder()
                .withEmail("test3@mail.com")
                .withPassword(passwordEncoder.encode("1234"))
                .build()
        authUserRepository.store(authUser)
        UserLoginInformation userInformation = new UserLoginInformation("test3@mail.com", "1234")
        and: "Token generator create a token with a1234 value"
        when(tokenGenerator.create(any())).thenReturn(new Token("a1234"))

        when:
        def response = mockMvc.perform(post("/api/authentications/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userInformation)))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == [
                    'token': 'a1234'
            ]
        }
    }

    void "it should return not found when user does not exist"() {
        given: "A user that doesn't exist"
        UserLoginInformation information = new UserLoginInformation("test4@mail.com", "1234")

        when:
        def response = mockMvc.perform(post("/api/authentications/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(information)))
                .andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'NOT_FOUND',
                            'error'  : 'USER_NOT_FOUND',
                            'message': 'There is no user with email provided'
                    ]
            ]
        }
    }

    void "it should return bad request when user is not active"() {
        given: 'A user that is not active'
        AuthUser authUser = new AuthUserBuilder()
                .withPassword(passwordEncoder.encode("1234"))
                .withStatus(Status.PENDING)
                .build()
        authUserRepository.store(authUser)
        UserLoginInformation information = new UserLoginInformation(authUser.email(), "1234")

        when:
        def response = mockMvc.perform(post("/api/authentications/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(information)))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [[
                                  'code'   : 'INVALID',
                                  'error'  : 'NON_ACTIVE_USER',
                                  'message': "The user is not active in the system"
                          ]]
        }
    }

    void "it should return bad request when password is incorrect"() {
        given: 'A user with password 1234'
        AuthUser authUser = new AuthUserBuilder()
                .withEmail("test5@mail.com")
                .withPassword(passwordEncoder.encode("1234"))
                .build()
        authUserRepository.store(authUser)

        when: 'Password sent is 12'
        UserLoginInformation userInformation = new UserLoginInformation("test5@mail.com", "12")
        def response = mockMvc.perform(post("/api/authentications/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userInformation)))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INVALID_PASSWORD',
                            'message': 'Password is incorrect'
                    ]
            ]
        }
    }

    void "it should return ok when user can logout successfully"() {
        when:
        def response = mockMvc.perform(delete('/api/authentications/123124as/')).andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and: 'Token is stored in token block list'
        tokenRepository.find("123124as").isDefined()
    }
}

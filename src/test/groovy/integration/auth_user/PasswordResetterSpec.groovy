package integration.auth_user

import authentication.application.dto.user.PasswordResetInformation
import authentication.domain.token.TokenGenerator
import authentication.domain.token.TokenRepository
import authentication.domain.user.AuthUserRepository
import authentication.domain.user.PasswordEncoder
import authentication.factories.AuthUserBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import server.WorkfficeApplication
import spock.lang.Specification

import static controller.IntegrationTestUtil.asJsonString
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class PasswordResetterSpec extends Specification {
    @Autowired
    AuthUserRepository authUserRepo
    @Autowired
    TokenRepository tokenRepo
    @Autowired
    PasswordEncoder passwordEncoder
    @Autowired
    TokenGenerator tokenGenerator
    @Autowired
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper

    void "it should return bad request when token is invalid"() {
        given:
        def passwordResetInformation = PasswordResetInformation.of("1234")

        when:
        def response = mockMvc
                .perform(post("/api/confirmation_tokens/password_resets/-1/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(passwordResetInformation)))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INVALID_TOKEN',
                            'message': 'Token provided is invalid or maybe has expired'
                    ]
            ]
        }
    }

    void "it should return bad request when token was already used"() {
        given: 'A token already used'
        def passwordResetInformation = PasswordResetInformation.of("1234")
        def authUser = new AuthUserBuilder().build()
        def token = tokenGenerator.create(authUser)
        authUserRepo.store(authUser)
        tokenRepo.store(token)

        when:
        def response = mockMvc
                .perform(post("/api/confirmation_tokens/password_resets/${token.token()}/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(passwordResetInformation)))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'TOKEN_ALREADY_USED',
                            'message': 'Token provided was already used'
                    ]
            ]
        }
    }

    void "it should return bad request when user does not exist"() {
        given: 'A token for a user that does not exist'
        def passwordResetInformation = PasswordResetInformation.of("1234")
        def authUser = new AuthUserBuilder().build()
        def token = tokenGenerator.create(authUser)

        when:
        def response = mockMvc
                .perform(post("/api/confirmation_tokens/password_resets/${token.token()}/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(passwordResetInformation)))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'USER_NOT_FOUND',
                            'message': 'There is no user associated with token'
                    ]
            ]
        }
    }

    void "it should return accepted when password is updated successfully"() {
        given: 'A token for an existent user'
        def passwordResetInformation = PasswordResetInformation.of("8122019")
        def authUser = new AuthUserBuilder().withPassword("1234").build()
        def token = tokenGenerator.create(authUser)
        authUserRepo.store(authUser)

        when:
        def response = mockMvc
                .perform(post("/api/confirmation_tokens/password_resets/${token.token()}/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(passwordResetInformation)))
                .andReturn().response

        then:
        response.status == HttpStatus.ACCEPTED.value()
        and:
        def authUserWithPassUpdated = authUserRepo.findById(authUser.id()).get()
        passwordEncoder.match("8122019", authUserWithPassUpdated.password())
    }

    void "it should return bad request when password information is invalid"() {
        given:
        def passwordResetInformation = PasswordResetInformation.of(invalidPassword)

        when:
        def response = mockMvc
                .perform(post("/api/confirmation_tokens/password_resets/12/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(passwordResetInformation)))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INVALID_PASSWORD',
                            'message': 'Password is required'
                    ]
            ]
        }

        where:
        invalidPassword << ["", null]
    }
}

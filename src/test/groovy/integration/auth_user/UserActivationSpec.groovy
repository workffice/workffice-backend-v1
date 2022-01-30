package integration.auth_user

import authentication.domain.token.Token
import authentication.domain.token.TokenGenerator
import authentication.domain.token.TokenRepository
import authentication.domain.user.AuthUser
import authentication.domain.user.AuthUserRepository
import authentication.domain.user.Status
import authentication.factories.AuthUserBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import server.WorkfficeApplication
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class UserActivationSpec extends Specification {

    @Autowired
    MockMvc mockMvc
    @Autowired
    AuthUserRepository authUserRepository
    @Autowired
    TokenRepository tokenRepository
    @Autowired
    TokenGenerator tokenGenerator
    @Autowired
    ObjectMapper objectMapper

    void "it should return bad request when token is invalid"() {
        when:
        def response = mockMvc
                .perform(post("/api/confirmation_tokens/account_activations/123/"))
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

    void "it should return bad request when user does not exist"() {
        given:
        AuthUser authUser = new AuthUserBuilder().build()
        Token token = tokenGenerator.create(authUser)

        when:
        def response = mockMvc
                .perform(post("/api/confirmation_tokens/account_activations/${token.token()}/"))
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

    void "it should return when activation token is already used"() {
        given:
        Token token = tokenGenerator.create(new AuthUserBuilder().build())
        tokenRepository.store(token)

        when:
        def response = mockMvc
                .perform(post("/api/confirmation_tokens/account_activations/${token.token()}/"))
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

    void "it should return ok when user is activated successfully"() throws Exception {
        given:
        AuthUser authUser = new AuthUserBuilder().withStatus(Status.PENDING).build()
        Token token = tokenGenerator.create(authUser)
        authUserRepository.store(authUser)

        when:
        def response = mockMvc
                .perform(post("/api/confirmation_tokens/account_activations/${token.token()}/"))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
    }
}

package integration.auth_user

import authentication.domain.user.AuthUserRepository
import authentication.factories.AuthUserBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import controller.IntegrationTestUtil
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
import spock.lang.Specification

import static controller.IntegrationTestUtil.asJsonString
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@SpringBootTest
@ContextConfiguration(classes = [WorkfficeApplication])
@AutoConfigureMockMvc
class ResetPasswordRequestSpec extends Specification {
    @Autowired
    AuthUserRepository authUserRepo
    @Autowired
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper
    @MockBean
    EmailSender emailSender


    void "it should return bad request when user_email is not specified"() {

        when:
        def response = mockMvc
                .perform(post('/api/users/password_reset_requests/').contentType(MediaType.APPLICATION_JSON).content(asJsonString(body)))
                .andReturn().response
        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INVALID_USEREMAIL',
                            'message': expectedError,
                    ]
            ]
        }
        where:
        body                         || expectedError
        new HashMap()                || 'Email is required'
        ['userEmail': null]          || 'Email is required'
        ['userEmail': '']            || 'Email is required'
        ['userEmail': 321321]        || 'Email format is invalid'
        ['userEmail': 'mailInvalid'] || 'Email format is invalid'
    }

    void "it should return not found when there is no user with email provided"() {

        given:
        def body = ["userEmail": "nonexistent@mail.com"]

        when:
        def response = mockMvc
                .perform(post('/api/users/password_reset_requests/').contentType(MediaType.APPLICATION_JSON).content(asJsonString(body)))
                .andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()

        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'NOT_FOUND',
                            'error'  : 'USER_NOT_FOUND',
                            'message': 'There is no user with email requested'
                    ]
            ]
        }
    }

    void "it should return accepted and send an email to the specified user email"() {
        given:
        def authUser = new AuthUserBuilder().build()
        authUserRepo.store(authUser)
        def body = ["userEmail": authUser.email()]

        when:
        def response = mockMvc
                .perform(post('/api/users/password_reset_requests/').contentType(MediaType.APPLICATION_JSON).content(asJsonString((body))))
                .andReturn().response

        then:
        response.status == HttpStatus.ACCEPTED.value()
        and:
        verify(emailSender, times(1)).send(any())
    }
}

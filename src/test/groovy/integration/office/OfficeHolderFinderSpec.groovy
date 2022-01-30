package integration.office

import authentication.application.dto.token.Authentication
import backoffice.domain.office_holder.OfficeHolder
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.factories.OfficeHolderBuilder
import com.fasterxml.jackson.databind.ObjectMapper
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
class OfficeHolderFinderSpec extends Specification {
    @Autowired
    MockMvc mockMvc
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    AuthUtil authUtil
    @Autowired
    ObjectMapper objectMapper

    void "it should return ok when office holder exists"() {
        given: 'An authenticated user with same email as office holder'
        OfficeHolder officeHolder = new OfficeHolderBuilder().build()
        officeHolderRepo.store(officeHolder)
        Authentication authentication = authUtil.createAndLoginUser(officeHolder.email(), "1234")

        when:
        String id = officeHolder.id().toString()
        def response = mockMvc.perform(get("/api/office_holders/${id}/")
                .header("Authorization", "Bearer " + authentication.getToken()))
                .andReturn().response

        then:
        response.status == HttpStatus.OK.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.data == [
                    'id'   : officeHolder.id().toString(),
                    'email': officeHolder.email()
            ]
        }
    }

    void "it should return bad request when office holder id does not have uuid format"() {
        given:
        Authentication authentication = authUtil.createAndLoginUser("marcelo@gallardo.com", "1234");

        when:
        def response = mockMvc.perform(get("/api/office_holders/-1/")
                .header("Authorization", "Bearer " + authentication.getToken()))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'INVALID',
                            'error'  : 'INVALID_ID',
                            'message': 'Id provided is invalid'
                    ]
            ]
        }
    }

    void "it should return not found when office holder does not exist"() {
        given:
        Authentication authentication = authUtil.createAndLoginUser("marcelo@gallardo.com", "1234");

        when:
        def response = mockMvc.perform(get("/api/office_holders/${UUID.randomUUID()}/")
                .header("Authorization", "Bearer " + authentication.getToken()))
                .andReturn().response

        then:
        response.status == HttpStatus.NOT_FOUND.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'NOT_FOUND',
                            'error'  : 'OFFICE_HOLDER_NOT_FOUND',
                            'message': 'There is no office holder with specified id'
                    ]
            ]
        }
    }

    void 'it should return forbidden when auth user does not have the same email as office holder'() {
        given: 'An office holder with email john@doe.com'
        OfficeHolder officeHolder = new OfficeHolderBuilder()
                .withEmail('john@doe.com')
                .build()
        officeHolderRepo.store(officeHolder)
        and: 'An authenticated user with email marcelo@gallardo.com'
        Authentication authentication = authUtil.createAndLoginUser("marcelo@gallardo.com", "1234");

        when: 'Auth user tries to access office holder'
        def response = mockMvc.perform(get("/api/office_holders/${officeHolder.id()}/")
                .header("Authorization", "Bearer " + authentication.getToken()))
                .andReturn().response

        then:
        response.status == HttpStatus.FORBIDDEN.value()
        and:
        with(objectMapper.readValue(response.contentAsString, Map)) {
            it.errors == [
                    [
                            'code'   : 'FORBIDDEN',
                            'error'  : 'OFFICE_HOLDER_FORBIDDEN',
                            'message': 'You do not have access to this resource'
                    ]
            ]
        }
    }
}

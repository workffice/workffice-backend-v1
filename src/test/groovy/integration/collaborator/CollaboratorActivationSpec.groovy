package integration.collaborator

import backoffice.domain.collaborator.CollaboratorRepository
import backoffice.domain.collaborator.CollaboratorTokenGenerator
import backoffice.domain.collaborator.Status
import backoffice.domain.office_branch.OfficeBranchRepository
import backoffice.domain.office_holder.OfficeHolderRepository
import backoffice.factories.CollaboratorBuilder
import backoffice.factories.OfficeBranchBuilder
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
class CollaboratorActivationSpec extends Specification {
    @Autowired
    CollaboratorRepository collaboratorRepo
    @Autowired
    OfficeHolderRepository officeHolderRepo
    @Autowired
    OfficeBranchRepository officeBranchRepo
    @Autowired
    CollaboratorTokenGenerator collaboratorTokenGenerator
    @Autowired
    MockMvc mockMvc
    @Autowired
    ObjectMapper objectMapper

    void "it should return bad request when token is invalid"() {
        when:
        def response = mockMvc
                .perform(post("/api/confirmation_tokens/collaborator_activations/-1/"))
                .andReturn().response

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
    }

    void "it should return accepted when collaborator is activated successfully"() {
        given: 'A collaborator in pending status'
        def officeBranch = new OfficeBranchBuilder().build()
        def collaborator = new CollaboratorBuilder()
                .withStatus(Status.PENDING)
                .withOfficeBranch(officeBranch).build()
        officeHolderRepo.store(officeBranch.owner())
        officeBranchRepo.store(officeBranch)
        collaboratorRepo.store(collaborator)
        and: 'A token for the collaborator in pending status'
        def token = collaboratorTokenGenerator.createToken(collaborator)

        when:
        def response = mockMvc
                .perform(post("/api/confirmation_tokens/collaborator_activations/${token.token}/"))
                .andReturn().response

        then:
        response.status == HttpStatus.ACCEPTED.value()
        and:
        def collaboratorUpdated = collaboratorRepo.findById(collaborator.id()).get()
        collaboratorUpdated.isActive()
    }
}

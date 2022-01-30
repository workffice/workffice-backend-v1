package shared.domain.email.template;

import lombok.EqualsAndHashCode;

import java.util.HashMap;

import static java.lang.String.format;

@EqualsAndHashCode(of = {"officeBranchName", "confirmationToken"})
public class CollaboratorInvitationTemplate implements Template {
    public static String TEMPLATE_NAME = "COLLABORATOR_INVITATION";
    private final String host;
    private final String officeBranchName;
    private final String confirmationToken;

    public CollaboratorInvitationTemplate(String host, String officeBranchName, String confirmationToken) {
        this.host              = host;
        this.officeBranchName  = officeBranchName;
        this.confirmationToken = confirmationToken;
    }

    @Override
    public String templateName() {
        return TEMPLATE_NAME;
    }

    @Override
    public String subject() {
        return "Fuiste invitado para colaborar en " + officeBranchName;
    }

    @Override
    public String from() {
        return "workffice.ar@gmail.com";
    }

    @Override
    public HashMap<String, String> substitutionData() {
        return new HashMap<>() {{
            put("url", format("%s/auth/collaborator/activate/?token=%s", host, confirmationToken));
            put("office_branch_name", officeBranchName);
        }};
    }

    @Override
    public String plainTextBody() {
        String link = format("%s/accept_invitation/?token=%s", host, confirmationToken);
        return "Para unirte a la sucursal por favor haz click en el siguiente link " + link;
    }
}

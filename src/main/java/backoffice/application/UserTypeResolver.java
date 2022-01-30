package backoffice.application;

import backoffice.domain.collaborator.CollaboratorRepository;
import backoffice.domain.office_holder.OfficeHolderRepository;

import org.springframework.stereotype.Service;

@Service
public class UserTypeResolver {
    private final OfficeHolderRepository officeHolderRepo;
    private final CollaboratorRepository collaboratorRepo;

    public UserTypeResolver(
            OfficeHolderRepository officeHolderRepo,
            CollaboratorRepository collaboratorRepo
    ) {
        this.officeHolderRepo = officeHolderRepo;
        this.collaboratorRepo = collaboratorRepo;
    }

    public UserType getUserType(String email) {
        if (officeHolderRepo.find(email).isDefined())
            return UserType.OFFICE_HOLDER;
        else if (!collaboratorRepo.find(email).isEmpty())
            return UserType.COLLABORATOR;
        else
            return UserType.RENTER;
    }

    public enum UserType {
        OFFICE_HOLDER,
        COLLABORATOR,
        RENTER,
    }
}

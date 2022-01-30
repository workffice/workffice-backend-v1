package backoffice.application.membership;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.membership.MembershipError;
import backoffice.domain.membership.MembershipId;
import backoffice.domain.membership.MembershipRepository;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import io.vavr.control.Either;

import org.springframework.stereotype.Service;

@Service
public class MembershipDeleter {

    private final MembershipRepository membershipRepo;
    private final PermissionValidator  permissionValidator;

    public MembershipDeleter(MembershipRepository membershipRepo, PermissionValidator permissionValidator) {
        this.membershipRepo      = membershipRepo;
        this.permissionValidator = permissionValidator;
    }

    public Either<MembershipError, Void> delete(MembershipId id) {
        return membershipRepo.findById(id)
                .toEither(MembershipError.MEMBERSHIP_NOT_FOUND)
                .filterOrElse(
                        membership -> permissionValidator.userHasPerms(
                                membership.officeBranch(),
                                Permission.create(Access.WRITE, Resource.MEMBERSHIP)
                        ), m -> MembershipError.MEMBERSHIP_FORBIDDEN)
                .map(membership -> {
                    membership.delete();
                    return membership;
                }).flatMap(membership -> membershipRepo.update(membership).toEither(MembershipError.DB_ERROR));
    }
}

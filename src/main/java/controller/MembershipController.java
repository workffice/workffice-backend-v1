package controller;

import backoffice.application.dto.membership.MembershipError;
import backoffice.application.dto.membership.MembershipInformation;
import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.membership.MembershipCreator;
import backoffice.application.membership.MembershipDeleter;
import backoffice.application.membership.MembershipFinder;
import backoffice.application.membership.MembershipUpdater;
import backoffice.domain.membership.MembershipId;
import backoffice.domain.office_branch.OfficeBranchId;
import controller.response.DataResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static java.lang.String.format;

@RestController
public class MembershipController extends BaseController {
    @Autowired
    MembershipCreator creator;
    @Autowired
    MembershipFinder finder;
    @Autowired
    MembershipDeleter deleter;
    @Autowired
    MembershipUpdater updater;

    ResponseEntity<DataResponse> notFound = ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(notFound("MEMBERSHIP_NOT_FOUND", "The membership requested does not exist"));
    ResponseEntity<DataResponse> forbidden = ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(forbidden("MEMBERSHIP_FORBIDDEN", "You don't have access to the office branch memberships"));
    ResponseEntity<DataResponse> officeBranchNotFound = ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(notFound("OFFICE_BRANCH_NOT_FOUND", "The office branch requested does not exist"));
    ResponseEntity<DataResponse> invalidId = ResponseEntity
            .badRequest()
            .body(invalid("INVALID_MEMBERSHIP_ID", "The membership id provided is invalid"));
    ResponseEntity<DataResponse> invalidOfficeBranchId = ResponseEntity
            .badRequest()
            .body(invalid("INVALID_OFFICE_BRANCH_ID", "The office branch id provided is invalid"));

    @PostMapping("/api/office_branches/{id}/memberships/")
    public ResponseEntity<?> createMembership(
            @PathVariable String id,
            @RequestBody MembershipInformation info
    ) {
        try {
            var officeBranchId = OfficeBranchId.fromString(id);
            var membershipId = new MembershipId();
            return creator.create(officeBranchId, membershipId, info)
                    .map(v -> ResponseEntity
                            .status(HttpStatus.CREATED)
                            .body((DataResponse) entityCreated(format("/api/memberships/%s/", membershipId))))
                    .getOrElseGet(error -> Match(error).of(
                            Case($(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST), officeBranchNotFound),
                            Case($(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN), forbidden)
                    ));
        } catch (IllegalArgumentException e) {
            return invalidOfficeBranchId;
        }
    }

    @GetMapping("/api/office_branches/{id}/memberships/")
    public ResponseEntity<?> getMemberships(@PathVariable String id) {
        try {
            var officeBranchId = OfficeBranchId.fromString(id);
            return finder.findByOfficeBranch(officeBranchId)
                    .map(memberships -> ResponseEntity
                            .ok((DataResponse) entityResponse(memberships)))
                    .getOrElse(officeBranchNotFound);
        } catch (IllegalArgumentException e) {
            return invalidOfficeBranchId;
        }
    }

    @PutMapping("/api/memberships/{id}/")
    public ResponseEntity<?> updateMembership(
            @PathVariable String id,
            @RequestBody MembershipInformation info
    ) {
        try {
            var membershipId = MembershipId.fromString(id);
            return updater.update(membershipId, info)
                    .map(v -> ResponseEntity.accepted().body((DataResponse) entityResponse("Success :'(")))
                    .getOrElseGet(error -> Match(error).of(
                            Case($(MembershipError.MEMBERSHIP_NOT_FOUND), notFound),
                            Case($(MembershipError.MEMBERSHIP_FORBIDDEN), forbidden)
                    ));
        } catch (IllegalArgumentException e) {
            return invalidId;
        }
    }

    @DeleteMapping("/api/memberships/{id}/")
    public ResponseEntity<?> deleteMembership(@PathVariable String id) {
        try {
            var membershipId = MembershipId.fromString(id);
            return deleter.delete(membershipId)
                    .map(v -> ResponseEntity.accepted().body((DataResponse) entityResponse("Success :'(")))
                    .getOrElseGet(error -> Match(error).of(
                            Case($(MembershipError.MEMBERSHIP_NOT_FOUND), notFound),
                            Case($(MembershipError.MEMBERSHIP_FORBIDDEN), forbidden)
                    ));
        } catch (IllegalArgumentException e) {
            return invalidId;
        }
    }
}

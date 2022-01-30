package controller;

import backoffice.application.dto.office_branch.OfficeBranchInformation;
import backoffice.application.dto.office_branch.OfficeBranchResponse;
import backoffice.application.dto.office_holder.OfficeHolderError;
import backoffice.application.dto.office_holder.OfficeHolderResponse;
import backoffice.application.office_branch.OfficeBranchCreator;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.application.office_holder.OfficeHolderFinder;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.office_holder.OfficeHolderId;
import controller.response.DataResponse;
import controller.response.ErrorResponse;
import controller.response.SingleResponse;
import io.vavr.control.Either;
import io.vavr.control.Option;
import shared.application.UseCaseError;

import java.util.List;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static backoffice.application.dto.office_holder.OfficeHolderError.OFFICE_HOLDER_FORBIDDEN;
import static backoffice.application.dto.office_holder.OfficeHolderError.OFFICE_HOLDER_NOT_FOUND;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping(value = "/api/office_holders")
public class OfficeHolderController extends BaseController {

    private final ErrorResponse notFound = notFound(
            "OFFICE_HOLDER_NOT_FOUND",
            "There is no office holder with specified id"
    );
    private final ErrorResponse forbidden = forbidden(
            "OFFICE_HOLDER_FORBIDDEN",
            "You do not have access to this resource"
    );
    private final ErrorResponse invalidId = invalid("INVALID_ID", "Id provided is invalid");

    @Autowired
    OfficeHolderFinder finder;
    @Autowired
    OfficeBranchCreator officeBranchCreator;
    @Autowired
    OfficeBranchFinder officeBranchFinder;

    @GetMapping(value = "/{id}/")
    public ResponseEntity<?> getOne(@PathVariable String id) {
        Function<OfficeHolderId, Either<UseCaseError, OfficeHolderResponse>> useCase =
                officeHolderId -> finder.find(officeHolderId);
        Function<OfficeHolderResponse, ResponseEntity<DataResponse>> handleSuccess =
                officeHolder -> ResponseEntity.ok(new SingleResponse<>(officeHolder));
        Function<UseCaseError, ResponseEntity<DataResponse>> handleError =
                error -> Match(error).of(
                        Case($(OFFICE_HOLDER_NOT_FOUND), ResponseEntity.status(NOT_FOUND).body(notFound)),
                        Case($(OFFICE_HOLDER_FORBIDDEN), ResponseEntity.status(FORBIDDEN).body(forbidden))
                );
        return processResponse(id, useCase, handleSuccess, handleError);
    }

    @PostMapping(value = "/{officeHolderId}/office_branches/")
    public ResponseEntity<?> createOfficeBranch(
            @PathVariable String officeHolderId, @RequestBody OfficeBranchInformation info
    ) {
        OfficeBranchId officeBranchId = new OfficeBranchId();
        DataResponse createdBody = entityCreated(format("/api/office_branches/%s/", officeBranchId));

        Function<OfficeHolderId, Either<UseCaseError, Void>> useCase =
                id -> officeBranchCreator.create(id, officeBranchId, info);
        Function<Void, ResponseEntity<DataResponse>> handleSuccess =
                v -> ResponseEntity.status(CREATED).body(createdBody);
        Function<UseCaseError, ResponseEntity<DataResponse>> handleError =
                error -> Match(error).of(
                        Case($(OFFICE_HOLDER_NOT_FOUND),
                                ResponseEntity.status(NOT_FOUND).body(notFound)),
                        Case($(OFFICE_HOLDER_FORBIDDEN),
                                ResponseEntity.status(FORBIDDEN).body(forbidden)));

        return processResponse(officeHolderId, useCase, handleSuccess, handleError);
    }

    @GetMapping(value = "/{id}/office_branches/")
    public ResponseEntity<?> getOfficeBranches(@PathVariable String id) {
        Function<OfficeHolderId, Either<OfficeHolderError, List<OfficeBranchResponse>>> useCase =
                officeHolderId -> officeBranchFinder.findByOfficeHolder(officeHolderId);
        Function<List<OfficeBranchResponse>, ResponseEntity<DataResponse>> handleSuccess =
                officeBranches -> ResponseEntity.ok(entityResponse(officeBranches));
        Function<OfficeHolderError, ResponseEntity<DataResponse>> handleError =
                error -> Match(error).of(
                        Case($(OFFICE_HOLDER_NOT_FOUND), ResponseEntity.status(NOT_FOUND).body(notFound))
                );
        return processResponse(id, useCase, handleSuccess, handleError);
    }

    private <E, S> ResponseEntity<DataResponse> processResponse(
            String id,
            Function<OfficeHolderId, Either<E, S>> useCase,
            Function<S, ResponseEntity<DataResponse>> handleSuccess,
            Function<E, ResponseEntity<DataResponse>> handleError
    ) {
        return parseOfficeHolderId(id)
                .map(useCase)
                .map(response -> response.fold(handleError, handleSuccess))
                .getOrElse(ResponseEntity.badRequest().body(invalidId));
    }

    private Option<OfficeHolderId> parseOfficeHolderId(String id) {
        try {
            return Option.of(OfficeHolderId.fromString(id));
        } catch (IllegalArgumentException e) {
            return Option.none();
        }
    }
}

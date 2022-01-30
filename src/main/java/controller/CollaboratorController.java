package controller;

import backoffice.application.collaborator.CollaboratorDeleter;
import backoffice.application.collaborator.CollaboratorFinder;
import backoffice.application.collaborator.CollaboratorUpdater;
import backoffice.application.dto.collaborator.CollaboratorError;
import backoffice.application.dto.collaborator.CollaboratorResponse;
import backoffice.application.dto.collaborator.CollaboratorUpdateInformation;
import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.dto.role.RoleResponse;
import backoffice.application.role.CollaboratorRolesFinder;
import backoffice.domain.collaborator.CollaboratorId;
import controller.response.DataResponse;
import io.vavr.control.Either;
import io.vavr.control.Option;
import shared.application.UseCaseError;

import java.util.List;
import java.util.function.Function;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

@RestController
@RequestMapping(value = "/api/collaborators")
public class CollaboratorController extends BaseController {

    ResponseEntity<DataResponse> notFound = ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(notFound("COLLABORATOR_NOT_FOUND", "There is no collaborator with id specified"));
    ResponseEntity<DataResponse> invalidId = ResponseEntity
            .badRequest()
            .body(invalid("INVALID_COLLABORATOR_ID", "The collaborator id specified has an invalid format"));
    ResponseEntity<DataResponse> forbidden = ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(forbidden("COLLABORATOR_FORBIDDEN", "You can't access to this collaborator"));

    @Autowired CollaboratorFinder      finder;
    @Autowired CollaboratorUpdater     updater;
    @Autowired CollaboratorRolesFinder collaboratorRolesFinder;
    @Autowired CollaboratorDeleter     deleter;

    private Option<CollaboratorId> parseId(String id) {
        try {
            return Option.of(CollaboratorId.fromString(id));
        } catch (IllegalArgumentException e) {
            return Option.none();
        }
    }

    private <E, S> ResponseEntity<?> processResponse(
            String id,
            Function<CollaboratorId, Either<E, S>> useCase,
            Function<S, ResponseEntity<DataResponse>> handleSuccess,
            Function<E, ResponseEntity<DataResponse>> handleError
    ) {
        return parseId(id)
                .map(useCase)
                .map(response -> response.fold(handleError, handleSuccess))
                .getOrElse(invalidId);
    }

    @GetMapping(value = "/{id}/")
    public ResponseEntity<?> getCollaborator(@PathVariable String id) {
        Function<CollaboratorId, Either<UseCaseError, CollaboratorResponse>> useCase =
                collaboratorId -> finder.find(collaboratorId);
        Function<CollaboratorResponse, ResponseEntity<DataResponse>> handleSuccess =
                collaborator -> ResponseEntity.ok(entityResponse(collaborator));
        Function<UseCaseError, ResponseEntity<DataResponse>> handleError =
                error -> Match(error).of(
                        Case($(CollaboratorError.COLLABORATOR_NOT_FOUND), notFound),
                        Case($(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN), forbidden)
                );
        return processResponse(id, useCase, handleSuccess, handleError);
    }

    @PutMapping(value = "/{id}/")
    public ResponseEntity<?> updateCollaborator(
            @PathVariable String id,
            @RequestBody @Valid CollaboratorUpdateInformation info
    ) {
        Function<CollaboratorId, Either<UseCaseError, Void>> useCase =
                collaboratorId -> updater.update(collaboratorId, info);
        Function<Void, ResponseEntity<DataResponse>> handleSuccess =
                v -> ResponseEntity.ok(entityResponse("success :'("));
        Function<UseCaseError, ResponseEntity<DataResponse>> handleError =
                error -> Match(error).of(
                        Case($(CollaboratorError.COLLABORATOR_NOT_FOUND), notFound),
                        Case($(CollaboratorError.FORBIDDEN), forbidden)
                );
        return processResponse(id, useCase, handleSuccess, handleError);
    }

    @GetMapping(value = "/{id}/roles/")
    public ResponseEntity<?> getCollaboratorRoles(@PathVariable String id) {
        Function<CollaboratorId, Either<UseCaseError, List<RoleResponse>>> useCase =
                collaboratorId -> collaboratorRolesFinder.find(collaboratorId);
        Function<List<RoleResponse>, ResponseEntity<DataResponse>> handleSuccess =
                roles -> ResponseEntity.ok(entityResponse(roles));
        Function<UseCaseError, ResponseEntity<DataResponse>> handleError =
                error -> Match(error).of(
                        Case($(CollaboratorError.COLLABORATOR_NOT_FOUND), notFound),
                        Case($(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN), forbidden)
                );
        return processResponse(id, useCase, handleSuccess, handleError);
    }

    @DeleteMapping(value = "/{id}/")
    public ResponseEntity<?> deleteCollaborator(@PathVariable String id) {
        Function<CollaboratorId, Either<CollaboratorError, Void>> useCase =
                collaboratorId -> deleter.delete(collaboratorId);
        Function<Void, ResponseEntity<DataResponse>> handleSuccess =
                roles -> ResponseEntity.ok(entityResponse("success :'("));
        Function<CollaboratorError, ResponseEntity<DataResponse>> handleError =
                error -> Match(error).of(
                        Case($(CollaboratorError.COLLABORATOR_NOT_FOUND), notFound),
                        Case($(CollaboratorError.FORBIDDEN), forbidden)
                );
        return processResponse(id, useCase, handleSuccess, handleError);
    }
}

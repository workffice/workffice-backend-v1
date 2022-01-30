package controller;

import backoffice.application.collaborator.CollaboratorCreator;
import backoffice.application.collaborator.CollaboratorsFinder;
import backoffice.application.dto.collaborator.CollaboratorError;
import backoffice.application.dto.collaborator.CollaboratorInformation;
import backoffice.application.dto.collaborator.CollaboratorResponse;
import backoffice.application.dto.equipment.EquipmentError;
import backoffice.application.dto.equipment.EquipmentInformation;
import backoffice.application.dto.equipment.EquipmentResponse;
import backoffice.application.dto.office.OfficeError;
import backoffice.application.dto.office.OfficeInformation;
import backoffice.application.dto.office.OfficeResponse;
import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.dto.office_branch.OfficeBranchUpdateInformation;
import backoffice.application.dto.role.RoleInformation;
import backoffice.application.dto.role.RoleResponse;
import backoffice.application.dto.service.ServiceError;
import backoffice.application.dto.service.ServiceInformation;
import backoffice.application.dto.service.ServiceResponse;
import backoffice.application.equipment.EquipmentCreator;
import backoffice.application.equipment.EquipmentFinder;
import backoffice.application.office.OfficeCreator;
import backoffice.application.office.OfficesFinder;
import backoffice.application.office_branch.OfficeBranchDeleter;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.application.office_branch.OfficeBranchUpdater;
import backoffice.application.office_branch.OfficeBranchesFinderByCollaborator;
import backoffice.application.role.RoleCreator;
import backoffice.application.role.RoleFinder;
import backoffice.application.service.ServiceCreator;
import backoffice.application.service.ServiceFinder;
import backoffice.domain.collaborator.CollaboratorId;
import backoffice.domain.equipment.EquipmentId;
import backoffice.domain.office.OfficeId;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.role.RoleId;
import backoffice.domain.service.ServiceId;
import controller.response.DataResponse;
import controller.response.SingleResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static java.lang.String.format;

@RestController
@RequestMapping(value = "/api/office_branches")
public class OfficeBranchController extends BaseController {
    private final ResponseEntity<DataResponse> notFound = ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(notFound("OFFICE_BRANCH_NOT_FOUND", "Office branch requested does not exist"));
    private final ResponseEntity<DataResponse> forbidden = ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(forbidden("OFFICE_BRANCH_FORBIDDEN", "You do not have access to office branch"));
    private final ResponseEntity<DataResponse> invalidId = ResponseEntity
            .badRequest()
            .body(invalid("INVALID_OFFICE_BRANCH_ID", "Office branch id is invalid"));

    @Autowired
    private OfficeCreator officeCreator;
    @Autowired
    private OfficeBranchFinder finder;
    @Autowired
    private OfficeBranchUpdater updater;
    @Autowired
    private OfficeBranchDeleter deleter;
    @Autowired
    private RoleCreator roleCreator;
    @Autowired
    private RoleFinder roleFinder;
    @Autowired
    private CollaboratorCreator collaboratorCreator;
    @Autowired
    private OfficesFinder officesFinder;
    @Autowired
    private CollaboratorsFinder collaboratorsFinder;
    @Autowired
    private OfficeBranchesFinderByCollaborator officeBranchesFinderByCollaborator;
    @Autowired
    private EquipmentCreator equipmentCreator;
    @Autowired
    private EquipmentFinder equipmentFinder;
    @Autowired
    private ServiceCreator serviceCreator;
    @Autowired
    private ServiceFinder serviceFinder;

    private Option<OfficeBranchId> parseId(String id) {
        try {
            return Option.of(OfficeBranchId.fromString(id));
        } catch (IllegalArgumentException e) {
            return Option.none();
        }
    }

    @GetMapping("/")
    public ResponseEntity<?> getOfficeBranchesByCollaborator(
            @RequestParam(name = "collaborator_email") String collaboratorEmail
    ) {
        ResponseEntity<DataResponse> collaboratorForbidden = ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(forbidden(
                        "COLLABORATOR_FORBIDDEN",
                        "You don't have access to collaborator office branches"
                ));
        return officeBranchesFinderByCollaborator.find(collaboratorEmail)
                .fold(
                        error -> Match(error).of(
                                Case($(CollaboratorError.FORBIDDEN), collaboratorForbidden)
                        ),
                        officeBranches -> ResponseEntity.ok(entityResponse(officeBranches))
                );
    }

    @GetMapping(value = "/{id}/")
    public ResponseEntity<?> getOfficeBranch(@PathVariable String id) {
        Option<OfficeBranchId> officeBranchId = parseId(id);

        if (officeBranchId.isEmpty())
            return invalidId;
        return finder
                .find(officeBranchId.get())
                .map(officeBranch -> ResponseEntity.ok((DataResponse) entityResponse(officeBranch)))
                .getOrElse(notFound);
    }

    @PutMapping(value = "/{id}/")
    public ResponseEntity<?> updateOfficeBranch(
            @PathVariable String id,
            @RequestBody OfficeBranchUpdateInformation info
    ) {
        Function<OfficeBranchId, Either<OfficeBranchError, Void>> useCase =
                officeBranchId -> updater.update(officeBranchId, info);
        Function<Void, ResponseEntity<DataResponse>> handleSuccess =
                v -> ResponseEntity.ok(entityResponse("success :'("));
        Function<OfficeBranchError, ResponseEntity<DataResponse>> handleError =
                error -> Match(error).of(
                        Case($(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST), notFound),
                        Case($(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN), forbidden)
                );
        return processResponse(id, useCase, handleSuccess, handleError);
    }

    @DeleteMapping(value = "/{id}/")
    public ResponseEntity<?> deleteOfficeBranch(@PathVariable String id) {
        Function<OfficeBranchId, Either<OfficeBranchError, Void>> useCase =
                officeBranchId -> deleter.delete(officeBranchId);
        Function<Void, ResponseEntity<DataResponse>> handleSuccess =
                v -> ResponseEntity.accepted().body(entityResponse("success :'("));

        ResponseEntity<DataResponse> officeBranchHasCreatedOffices = ResponseEntity
                .badRequest()
                .body(invalid("OFFICE_BRANCH_HAS_CREATED_OFFICES", "Office branch cannot be deleted because it has " +
                        "created offices"));
        Function<OfficeBranchError, ResponseEntity<DataResponse>> handleError =
                error -> Match(error).of(
                        Case($(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST), notFound),
                        Case($(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN), forbidden),
                        Case($(OfficeBranchError.OFFICE_BRANCH_HAS_CREATED_OFFICES), officeBranchHasCreatedOffices)
                );
        return processResponse(id, useCase, handleSuccess, handleError);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "/{id}/offices/")
    public ResponseEntity<?> createOffice(
            @PathVariable String id,
            @RequestBody @Valid OfficeInformation information
    ) {
        OfficeId officeId = new OfficeId();
        DataResponse createdResponse = entityCreated("/api/offices/" + officeId + "/");
        ResponseEntity<DataResponse> sharedOfficeError = ResponseEntity
                .badRequest()
                .body(invalid(
                        "SHARED_OFFICE_WITHOUT_TABLES",
                        "You must specify table information for a shared office"
                ));

        Function<OfficeBranchId, Either<UseCaseError, Void>> useCase =
                officeBranchId -> officeCreator.create(officeId, officeBranchId, information);
        Function<Void, ResponseEntity<DataResponse>> handleSuccess =
                v -> ResponseEntity.status(HttpStatus.CREATED).body(createdResponse);
        Function<UseCaseError, ResponseEntity<DataResponse>> handleError = error -> Match(error).of(
                Case($(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST), notFound),
                Case($(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN), forbidden),
                Case($(OfficeError.SHARED_OFFICE_WITHOUT_TABLES), sharedOfficeError)
        );

        return processResponse(id, useCase, handleSuccess, handleError);
    }

    @GetMapping(value = "/{id}/offices/")
    public ResponseEntity<?> getOffices(@PathVariable String id) {
        Function<OfficeBranchId, Either<UseCaseError, List<OfficeResponse>>> useCase =
                officeBranchId -> officesFinder.find(officeBranchId);
        Function<List<OfficeResponse>, ResponseEntity<DataResponse>> handleSuccess =
                offices -> ResponseEntity.ok(entityResponse(offices));
        Function<UseCaseError, ResponseEntity<DataResponse>> handleError =
                error -> Match(error).of(
                        Case($(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST), notFound)
                );
        return processResponse(id, useCase, handleSuccess, handleError);
    }

    @PostMapping(value = "/{id}/roles/")
    public ResponseEntity<?> createRole(
            @PathVariable String id,
            @Valid @RequestBody RoleInformation roleInformation
    ) {
        var roleId = new RoleId();
        Function<OfficeBranchId, Either<UseCaseError, Void>> useCase =
                officeBranchId -> roleCreator.createRole(officeBranchId, roleId, roleInformation);
        Function<Void, ResponseEntity<DataResponse>> handleSuccess = v -> {
            var body = entityCreated(format("/api/roles/%s/", roleId));
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        };
        Function<UseCaseError, ResponseEntity<DataResponse>> handleError = error -> Match(error).of(
                Case($(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST), notFound),
                Case($(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN), forbidden)
        );
        return processResponse(id, useCase, handleSuccess, handleError);
    }

    @GetMapping(value = "/{id}/roles/")
    public ResponseEntity<?> getRoles(@PathVariable String id) {
        Function<OfficeBranchId, Either<UseCaseError, List<RoleResponse>>> useCase =
                officeBranchId -> roleFinder.findRoles(officeBranchId);
        Function<List<RoleResponse>, ResponseEntity<DataResponse>> handleSuccess =
                roles -> ResponseEntity.ok(new SingleResponse<>(roles));
        Function<UseCaseError, ResponseEntity<DataResponse>> handleError = error -> Match(error).of(
                Case($(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST), notFound),
                Case($(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN), forbidden)
        );
        return processResponse(id, useCase, handleSuccess, handleError);
    }

    @PostMapping(value = "/{id}/collaborators/")
    public ResponseEntity<?> createCollaborator(
            @PathVariable String id,
            @RequestBody @Valid CollaboratorInformation collaboratorInformation
    ) {
        var collaboratorAlreadyExistError = invalid(
                "COLLABORATOR_ALREADY_EXISTS",
                "There is a collaborator with the same email for the office branch requested"
        );
        var collaboratorId = new CollaboratorId();
        Function<OfficeBranchId, Either<UseCaseError, Void>> useCase =
                officeBranchId -> collaboratorCreator.create(officeBranchId, collaboratorId, collaboratorInformation);
        Function<Void, ResponseEntity<DataResponse>> handleSuccess = v -> {
            var body = entityCreated(format("/api/collaborators/%s/", collaboratorId));
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        };
        Function<UseCaseError, ResponseEntity<DataResponse>> handleError = error -> Match(error).of(
                Case($(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN), forbidden),
                Case($(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST), notFound),
                Case($(CollaboratorError.COLLABORATOR_ALREADY_EXISTS), ResponseEntity
                        .badRequest().body(collaboratorAlreadyExistError))
        );
        return processResponse(id, useCase, handleSuccess, handleError);
    }

    @GetMapping(value = "/{id}/collaborators/")
    public ResponseEntity<?> getCollaborators(@PathVariable String id) {
        Function<OfficeBranchId, Either<UseCaseError, List<CollaboratorResponse>>> useCase =
                officeBranchId -> collaboratorsFinder.find(officeBranchId);
        Function<List<CollaboratorResponse>, ResponseEntity<DataResponse>> handleSuccess =
                collaborators -> ResponseEntity.ok(entityResponse(collaborators));
        Function<UseCaseError, ResponseEntity<DataResponse>> handleError =
                error -> Match(error).of(
                        Case($(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST), () -> notFound),
                        Case($(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN), () -> forbidden)
                );
        return processResponse(id, useCase, handleSuccess, handleError);
    }

    @PostMapping(value = "/{id}/equipments/")
    public ResponseEntity<?> createEquipment(
            @PathVariable String id,
            @Valid @RequestBody EquipmentInformation equipmentInformation) {
        EquipmentId equipmentId = new EquipmentId();
        DataResponse failureResponse = invalid(
                "EQUIPMENT_ALREADY_EXISTS", "There is another equipment with the same id");

        Function<OfficeBranchId, Either<UseCaseError, Void>> useCase =
                officeBranchId -> equipmentCreator.createEquipment(equipmentId, equipmentInformation, officeBranchId);
        Function<Void, ResponseEntity<DataResponse>> handleSuccess =
                v -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body((DataResponse) entityCreated("/api/equipments/" + equipmentId + "/"));
        Function<UseCaseError, ResponseEntity<DataResponse>> handleError =
                error -> Match(error).of(
                        Case($(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST), notFound),
                        Case($(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN), forbidden),
                        Case($(EquipmentError.EQUIPMENT_ALREADY_EXISTS),
                                ResponseEntity.badRequest().body(failureResponse))
                );
        return processResponse(id, useCase, handleSuccess, handleError);
    }

    @GetMapping(value = "/{id}/equipments/")
    public ResponseEntity<?> getEquipments(@PathVariable String id) {
        Function<OfficeBranchId, Either<UseCaseError, List<EquipmentResponse>>> useCase =
                officeBranchId -> equipmentFinder.find((officeBranchId));
        Function<List<EquipmentResponse>, ResponseEntity<DataResponse>> handleSuccess =
                equipments -> ResponseEntity.ok(entityResponse(equipments));
        Function<UseCaseError, ResponseEntity<DataResponse>> handleError = error -> Match(error).of(
                Case($(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST), notFound),
                Case($(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN), forbidden)
        );
        return processResponse(id, useCase, handleSuccess, handleError);
    }

    @PostMapping(value = "/{id}/services/")
    public ResponseEntity<?> createService(
            @PathVariable String id,
            @Valid @RequestBody ServiceInformation serviceInformation) {
        ServiceId serviceId = new ServiceId();
        DataResponse failureResponse = invalid(
                "SERVICE_ALREADY_EXISTS", "There is another service with the same id");

        Function<OfficeBranchId, Either<UseCaseError, Void>> useCase =
                officeBranchId -> serviceCreator.createService(serviceId, serviceInformation, officeBranchId);
        Function<Void, ResponseEntity<DataResponse>> handleSuccess =
                v -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body((DataResponse) entityCreated("/api/services/" + serviceId + "/"));
        Function<UseCaseError, ResponseEntity<DataResponse>> handleError =
                error -> Match(error).of(
                        Case($(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST), notFound),
                        Case($(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN), forbidden),
                        Case($(ServiceError.SERVICE_ALREADY_EXISTS),
                                ResponseEntity.badRequest().body(failureResponse))
                );
        return processResponse(id, useCase, handleSuccess, handleError);
    }

    @GetMapping(value = "/{id}/services/")
    public ResponseEntity<?> getService(@PathVariable String id) {
        Function<OfficeBranchId, Either<UseCaseError, List<ServiceResponse>>> useCase =
                officeBranchId -> serviceFinder.find((officeBranchId));
        Function<List<ServiceResponse>, ResponseEntity<DataResponse>> handleSuccess =
                services -> ResponseEntity.ok(entityResponse(services));
        Function<UseCaseError, ResponseEntity<DataResponse>> handleError = error -> Match(error).of(
                Case($(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST), notFound),
                Case($(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN), forbidden)
        );
        return processResponse(id, useCase, handleSuccess, handleError);
    }

    private <E, S> ResponseEntity<DataResponse> processResponse(
            String id,
            Function<OfficeBranchId, Either<E, S>> useCase,
            Function<S, ResponseEntity<DataResponse>> handleSuccess,
            Function<E, ResponseEntity<DataResponse>> handleError
    ) {
        return parseId(id)
                .map(useCase)
                .map(response -> response.fold(handleError, handleSuccess))
                .getOrElse(invalidId);
    }

}

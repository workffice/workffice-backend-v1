package controller;

import backoffice.application.dto.role.RoleError;
import backoffice.application.dto.role.RoleInformation;
import backoffice.application.role.RoleDeleter;
import backoffice.application.role.RoleUpdater;
import backoffice.domain.role.RoleId;
import controller.response.DataResponse;
import controller.response.SingleResponse;

import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

@RestController
@RequestMapping(value = "/api/roles")
public class RoleController extends BaseController {
    @Autowired RoleUpdater updater;
    @Autowired RoleDeleter deleter;

    ResponseEntity<DataResponse> roleForbidden = ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(forbidden("ROLE_FORBIDDEN", "You don't have access to the role requested"));
    ResponseEntity<DataResponse> roleNotFound = ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(notFound("ROLE_NOT_FOUND", "There is no role with id specified"));
    ResponseEntity<DataResponse> invalidId = ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(invalid("INVALID_ROLE_ID", "The role id specified is not valid"));

    @PutMapping(value = "/{id}/")
    public ResponseEntity<?> updateRole(
            @PathVariable String id,
            @RequestBody @Valid RoleInformation roleInformation
    ) {
        try {
            var roleId = RoleId.fromString(id);
            return updater
                    .update(roleId, roleInformation)
                    .map(role -> ResponseEntity.ok((DataResponse) new SingleResponse<>(role)))
                    .getOrElseGet(error -> Match(error).of(
                            Case($(RoleError.ROLE_FORBIDDEN), roleForbidden),
                            Case($(RoleError.ROLE_NOT_FOUND), roleNotFound)
                    ));
        } catch (IllegalArgumentException e) {
            return invalidId;
        }
    }

    @DeleteMapping(value = "/{id}/")
    public ResponseEntity<?> deleteRole(@PathVariable String id) {
        try {
            var roleId = RoleId.fromString(id);
            return deleter
                    .delete(roleId)
                    .map(role -> ResponseEntity.ok((DataResponse) entityResponse("success :'(")))
                    .getOrElseGet(error -> Match(error).of(
                            Case($(RoleError.ROLE_FORBIDDEN), roleForbidden),
                            Case($(RoleError.ROLE_NOT_FOUND), roleNotFound)
                    ));
        } catch (IllegalArgumentException e) {
            return invalidId;
        }
    }
}

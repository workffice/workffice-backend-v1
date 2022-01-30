package controller;

import authentication.application.AuthUserFinder;
import backoffice.application.dto.office.OfficeError;
import backoffice.application.dto.office.OfficeResponse;
import backoffice.application.dto.office.OfficeUpdateInformation;
import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.dto.office_inactivity.InactivityError;
import backoffice.application.dto.office_inactivity.InactivityInformation;
import backoffice.application.dto.office_inactivity.InactivityResponse;
import backoffice.application.office.OfficeDeleter;
import backoffice.application.office.OfficeEquipmentUpdater;
import backoffice.application.office.OfficeFinder;
import backoffice.application.office.OfficeServiceUpdater;
import backoffice.application.office.OfficeUpdater;
import backoffice.application.office_inactivity.InactivitiesFinder;
import backoffice.application.office_inactivity.InactivitiesUpdater;
import backoffice.application.office_inactivity.InactivityCreator;
import backoffice.domain.equipment.EquipmentId;
import backoffice.domain.office.OfficeId;
import backoffice.domain.office_inactivity.InactivityId;
import backoffice.domain.service.ServiceId;
import booking.application.booking.BookingByOfficeFinder;
import booking.application.booking.BookingCreator;
import booking.application.booking.BookingScheduleTimeFinder;
import booking.application.booking.creation.BookingFromMembershipCreator;
import booking.application.dto.booking.BookingError;
import booking.application.dto.booking.BookingInformation;
import booking.domain.booking.BookingRepository;
import booking.domain.membership_acquisiton.MembershipAcquisitionId;
import booking.domain.membership_acquisiton.MembershipAcquisitionRepository;
import booking.domain.office.OfficeRepository;
import controller.response.DataResponse;
import io.vavr.control.Either;
import io.vavr.control.Option;
import shared.application.UseCaseError;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import org.springframework.web.bind.annotation.RestController;

import static booking.application.dto.membership_acquisition.MembershipAcquisitionError.MEMBERSHIP_ACQUISITION_FORBIDDEN;
import static booking.application.dto.membership_acquisition.MembershipAcquisitionError.MEMBERSHIP_ACQUISITION_IS_NOT_ACTIVE;
import static booking.application.dto.membership_acquisition.MembershipAcquisitionError.MEMBERSHIP_ACQUISITION_NOT_FOUND;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static java.lang.String.format;

@RestController
@RequestMapping(value = "/api/offices")
public class OfficeController extends BaseController {

    private final ResponseEntity<DataResponse> notFound = ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(notFound("OFFICE_NOT_FOUND", "There is no office with id provided"));

    private final ResponseEntity<DataResponse> inactivityMismatchWithDate = ResponseEntity
            .badRequest()
            .body(invalid("INACTIVITY_TYPE_MISMATCH_WITH_DATE", "The inactivity does not match with the date"));

    private final ResponseEntity<DataResponse> forbidden = ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(forbidden("OFFICE_FORBIDDEN", "Forbidden you do not have access to office"));

    private final ResponseEntity<DataResponse> invalidId = ResponseEntity
            .badRequest()
            .body(invalid("INVALID_OFFICE_ID", "Office id provided has a wrong format"));

    private final ResponseEntity<DataResponse> sharedOfficeWithoutTables = ResponseEntity
            .badRequest()
            .body(invalid("SHARED_OFFICE_WITHOUT_TABLES", "You must specify table information for a shared office"));

    private final ResponseEntity<DataResponse> officeIsDeleted = ResponseEntity
            .badRequest()
            .body(invalid("OFFICE_IS_DELETED", "Office can't be booked because is deleted"));

    private final ResponseEntity<DataResponse> invalidScheduleTime = ResponseEntity
            .badRequest()
            .body(invalid(
                    "INVALID_SCHEDULE_TIME",
                    "Schedule time provided is invalid: " +
                            "endTime must be after startTime and both should be an exact hour without minutes"
            ));

    private final ResponseEntity<DataResponse> officeIsNotAvailable = ResponseEntity
            .badRequest()
            .body(invalid("OFFICE_IS_NOT_AVAILABLE", "Office is not available at schedule time specified"));

    @Autowired private OfficeFinder              finder;
    @Autowired private OfficeUpdater             updater;
    @Autowired private OfficeDeleter             deleter;
    @Autowired private InactivitiesFinder        inactivitiesFinder;
    @Autowired private InactivityCreator         inactivityCreator;
    @Autowired private InactivitiesUpdater       inactivitiesUpdater;
    @Autowired private OfficeServiceUpdater      officeServiceUpdater;
    @Autowired private OfficeEquipmentUpdater    officeEquipmentUpdater;
    @Autowired private BookingCreator            bookingCreator;
    @Autowired private BookingByOfficeFinder     bookingByOfficeFinder;
    @Autowired private BookingScheduleTimeFinder bookingScheduleTimeFinder;

    @Autowired private BookingRepository bookingRepo;
    @Autowired private MembershipAcquisitionRepository membershipAcquisitionRepo;
    @Autowired private AuthUserFinder authUserFinder;
    @Autowired private OfficeRepository officeRepo;

    @GetMapping(value = "/{id}/")
    public ResponseEntity<?> getOffice(@PathVariable String id) {
        Function<OfficeId, Either<UseCaseError, OfficeResponse>> useCase =
                officeId -> finder.find(officeId);
        Function<OfficeResponse, ResponseEntity<DataResponse>> handleSuccess =
                office -> ResponseEntity.ok((DataResponse) entityResponse(office));
        Function<UseCaseError, ResponseEntity<DataResponse>> handleError =
                error -> Match(error).of(
                        Case($(OfficeError.OFFICE_NOT_FOUND), notFound)
                );
        return processResponse(id, useCase, handleSuccess, handleError);
    }

    @PutMapping(value = "/{id}/")
    public ResponseEntity<?> updateOffice(
            @PathVariable String id,
            @RequestBody OfficeUpdateInformation info
    ) {
        Function<OfficeId, Either<UseCaseError, Void>> useCase =
                officeId -> updater.update(officeId, info);
        Function<Void, ResponseEntity<DataResponse>> handleSuccess =
                v -> ResponseEntity.ok(entityResponse("success :'("));
        Function<UseCaseError, ResponseEntity<DataResponse>> handleError =
                error -> Match(error).of(
                        Case($(OfficeError.OFFICE_NOT_FOUND), notFound),
                        Case($(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN), forbidden),
                        Case($(OfficeError.SHARED_OFFICE_WITHOUT_TABLES), sharedOfficeWithoutTables)
                );
        return processResponse(id, useCase, handleSuccess, handleError);
    }

    @DeleteMapping(value = "/{id}/")
    public ResponseEntity<?> deleteOffice(@PathVariable String id) {
        Function<OfficeId, Either<OfficeError, Void>> useCase =
                officeId -> deleter.delete(officeId);
        Function<Void, ResponseEntity<DataResponse>> handleSuccess =
                v -> ResponseEntity.accepted().body(entityResponse("success :'("));
        Function<OfficeError, ResponseEntity<DataResponse>> handleError =
                error -> Match(error).of(
                        Case($(OfficeError.OFFICE_NOT_FOUND), notFound),
                        Case($(OfficeError.OFFICE_FORBIDDEN), forbidden)
                );
        return processResponse(id, useCase, handleSuccess, handleError);
    }

    @GetMapping(value = "/{id}/inactivities/")
    public ResponseEntity<?> getInactivity(@PathVariable String id) {
        Function<OfficeId, Either<UseCaseError, List<InactivityResponse>>> useCase =
                officeId -> inactivitiesFinder.find(officeId);
        Function<List<InactivityResponse>, ResponseEntity<DataResponse>> handleSuccess =
                inactivities -> ResponseEntity.ok(entityResponse(inactivities));
        Function<UseCaseError, ResponseEntity<DataResponse>> handleError =
                error -> Match(error).of(
                        Case($(OfficeError.OFFICE_NOT_FOUND), notFound)
                );
        return processResponse(id, useCase, handleSuccess, handleError);
    }

    @PostMapping(value = "/{id}/inactivities/")
    public ResponseEntity<?> createInactivity(
            @PathVariable String id,
            @Valid @RequestBody InactivityInformation inactivityInformation
    ) {
        InactivityId inactivityId = new InactivityId();
        Function<OfficeId, Either<UseCaseError, Void>> useCase =
                officeId -> inactivityCreator.create(officeId, inactivityId, inactivityInformation);
        Function<Void, ResponseEntity<DataResponse>> handleSuccess =
                v -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body((DataResponse) entityCreated("/api/inactivities/" + inactivityId + "/"));
        Function<UseCaseError, ResponseEntity<DataResponse>> handleError =
                error -> Match(error).of(
                        Case($(OfficeError.OFFICE_NOT_FOUND), notFound),
                        Case($(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN), forbidden),
                        Case($(InactivityError.INACTIVITY_TYPE_MISMATCH_WITH_DATE), inactivityMismatchWithDate)
                );
        return processResponse(id, useCase, handleSuccess, handleError);
    }

    @PutMapping("/{id}/inactivities/")
    public ResponseEntity<?> updateInactivities(
            @PathVariable String id,
            @RequestBody List<InactivityInformation> infos
    ) {
        Function<OfficeId, Either<UseCaseError, Void>> useCase =
                officeId -> inactivitiesUpdater.updateOfficeInactivities(officeId, infos);
        Function<Void, ResponseEntity<DataResponse>> handleSuccess =
                v -> ResponseEntity.status(HttpStatus.ACCEPTED).body(entityResponse("Success :'("));
        Function<UseCaseError, ResponseEntity<DataResponse>> handleError =
                error -> Match(error).of(
                        Case($(OfficeError.OFFICE_NOT_FOUND), notFound),
                        Case($(OfficeError.OFFICE_FORBIDDEN), forbidden),
                        Case($(InactivityError.INACTIVITY_TYPE_MISMATCH_WITH_DATE), inactivityMismatchWithDate)
                );
        return processResponse(id, useCase, handleSuccess, handleError);
    }

    @PutMapping("/{id}/services/")
    public ResponseEntity<?> updateServices(
            @PathVariable String id,
            @RequestBody Set<String> serviceIds
    ) {
        try {
            var ids = serviceIds.stream().map(ServiceId::fromString).collect(Collectors.toSet());
            Function<OfficeId, Either<OfficeError, Void>> useCase =
                    officeId -> officeServiceUpdater.update(officeId, ids);
            Function<Void, ResponseEntity<DataResponse>> handleSuccess =
                    v -> ResponseEntity.status(HttpStatus.ACCEPTED).body(entityResponse("Success :'("));
            Function<OfficeError, ResponseEntity<DataResponse>> handleError =
                    error -> Match(error).of(
                            Case($(OfficeError.OFFICE_NOT_FOUND), notFound),
                            Case($(OfficeError.OFFICE_FORBIDDEN), forbidden)
                    );
            return processResponse(id, useCase, handleSuccess, handleError);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(invalid("INVALID_SERVICE_ID", "There is at least one invalid service id"));
        }
    }

    @PutMapping("/{id}/equipments/")
    public ResponseEntity<?> updateEquipments(
            @PathVariable String id,
            @RequestBody Set<String> equipmentIds
    ) {
        try {
            var ids = equipmentIds.stream().map(EquipmentId::fromString).collect(Collectors.toSet());
            Function<OfficeId, Either<OfficeError, Void>> useCase =
                    officeId -> officeEquipmentUpdater.update(officeId, ids);
            Function<Void, ResponseEntity<DataResponse>> handleSuccess =
                    v -> ResponseEntity.status(HttpStatus.ACCEPTED).body(entityResponse("Success :'("));
            Function<OfficeError, ResponseEntity<DataResponse>> handleError =
                    error -> Match(error).of(
                            Case($(OfficeError.OFFICE_NOT_FOUND), notFound),
                            Case($(OfficeError.OFFICE_FORBIDDEN), forbidden)
                    );
            return processResponse(id, useCase, handleSuccess, handleError);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(invalid("INVALID_EQUIPMENT_ID", "There is at least one invalid equipment id"));
        }
    }

    @PostMapping("/{id}/bookings/")
    public ResponseEntity<?> book(@PathVariable String id, @RequestBody BookingInformation info) {
        try {
            var officeId = booking.domain.office.OfficeId.fromString(id);
            var membershipAcquisitionId = info.getMembershipAcquisitionId();
            if (membershipAcquisitionId == null)
                return createSingleBooking(officeId, info);
            else
                return createBookingFromMembership(officeId, membershipAcquisitionId, info);
        } catch (IllegalArgumentException e) {
            return invalidId;
        }
    }

    private ResponseEntity<?> createBookingFromMembership(
            booking.domain.office.OfficeId officeId,
            String membershipAcquisitionId,
            BookingInformation info
    ) {
        var id = MembershipAcquisitionId.fromString(membershipAcquisitionId);
        var bookingCreationStrategy = new BookingFromMembershipCreator(bookingRepo, membershipAcquisitionRepo, id);
        var bookingCreator = new BookingCreator(authUserFinder, officeRepo, bookingRepo, bookingCreationStrategy);
        ResponseEntity<DataResponse> membershipAcquisitionNotFound = ResponseEntity
                .badRequest()
                .body(invalid(
                        "MEMBERSHIP_ACQUISITION_NOT_FOUND",
                        "There is no membership acquisition with id provided"
                ));
        ResponseEntity<DataResponse> membershipAcquisitionForbidden = ResponseEntity
                .badRequest()
                .body(invalid(
                        "MEMBERSHIP_ACQUISITION_FORBIDDEN",
                        "The membership acquisition id provided is not from your user"
                ));
        ResponseEntity<DataResponse> membershipAcquisitionIsNotActive = ResponseEntity
                .badRequest()
                .body(invalid("MEMBERSHIP_ACQUISITION_IS_NOT_ACTIVE", "The membership acquisition is not active"));
        return bookingCreator.create(officeId, info)
                .map(bookingResponse -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body((DataResponse) entityCreated(format("/api/bookings/%s/", bookingResponse.getId()))))
                .getOrElseGet(error -> Match(error).of(
                        Case($(booking.application.dto.OfficeError.OFFICE_NOT_FOUND), notFound),
                        Case($(booking.application.dto.OfficeError.OFFICE_IS_DELETED), officeIsDeleted),
                        Case($(BookingError.INVALID_SCHEDULE_TIME), invalidScheduleTime),
                        Case($(BookingError.OFFICE_IS_NOT_AVAILABLE), officeIsNotAvailable),
                        Case($(MEMBERSHIP_ACQUISITION_NOT_FOUND), membershipAcquisitionNotFound),
                        Case($(MEMBERSHIP_ACQUISITION_FORBIDDEN), membershipAcquisitionForbidden),
                        Case($(MEMBERSHIP_ACQUISITION_IS_NOT_ACTIVE), membershipAcquisitionIsNotActive)
                ));
    }

    private ResponseEntity<?> createSingleBooking(booking.domain.office.OfficeId officeId, BookingInformation info) {
        return bookingCreator.create(officeId, info)
                .map(bookingResponse -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body((DataResponse) entityCreated(format("/api/bookings/%s/", bookingResponse.getId()))))
                .getOrElseGet(error -> Match(error).of(
                        Case($(booking.application.dto.OfficeError.OFFICE_NOT_FOUND), notFound),
                        Case($(booking.application.dto.OfficeError.OFFICE_IS_DELETED), officeIsDeleted),
                        Case($(BookingError.INVALID_SCHEDULE_TIME), invalidScheduleTime),
                        Case($(BookingError.OFFICE_IS_NOT_AVAILABLE), officeIsNotAvailable)
                ));
    }

    @GetMapping("/{id}/bookings/")
    public ResponseEntity<?> getBookings(
            @PathVariable String id,
            @RequestParam String date
    ) {
        try {
            var queryDate = LocalDate.parse(date);
            var officeId = booking.domain.office.OfficeId.fromString(id);
            ResponseEntity<DataResponse> bookingForbidden = ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(forbidden("BOOKING_FORBIDDEN", "You don't have access to office bookings"));
            return bookingByOfficeFinder.find(officeId, queryDate)
                    .map(bookings -> ResponseEntity.ok((DataResponse) entityResponse(bookings)))
                    .getOrElseGet(error -> Match(error).of(
                            Case($(booking.application.dto.OfficeError.OFFICE_NOT_FOUND), notFound),
                            Case($(BookingError.BOOKING_FORBIDDEN), bookingForbidden)
                    ));
        } catch (IllegalArgumentException e) {
            return invalidId;
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(invalid(
                    "INVALID_DATE",
                    "The date format is invalid, must be aaaa-mm-dd"
            ));
        }
    }

    @GetMapping("/{id}/booking_scheduled_times/")
    public ResponseEntity<?> getBookingScheduleTimes(
            @PathVariable String id,
            @RequestParam String date
    ) {
        try {
            var queryDate = LocalDate.parse(date);
            var officeId = booking.domain.office.OfficeId.fromString(id);
            return bookingScheduleTimeFinder.findBookingScheduledTimes(officeId, queryDate)
                    .map(scheduleTimes -> ResponseEntity.ok((DataResponse) entityResponse(scheduleTimes)))
                    .getOrElseGet(error -> Match(error).of(
                            Case($(booking.application.dto.OfficeError.OFFICE_NOT_FOUND), notFound)
                    ));
        } catch (IllegalArgumentException e) {
            return invalidId;
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(invalid(
                    "INVALID_DATE",
                    "The date format is invalid, must be aaaa-mm-dd"
            ));
        }
    }

    private Option<OfficeId> parseId(String id) {
        try {
            return Option.of(OfficeId.fromString(id));
        } catch (IllegalArgumentException ignore) {
            return Option.none();
        }
    }

    private <E, S> ResponseEntity<?> processResponse(
            String id,
            Function<OfficeId, Either<E, S>> useCase,
            Function<S, ResponseEntity<DataResponse>> handleSuccess,
            Function<E, ResponseEntity<DataResponse>> handleError
    ) {
        return parseId(id)
                .map(useCase)
                .map(result -> result.fold(handleError, handleSuccess))
                .getOrElse(invalidId);
    }
}

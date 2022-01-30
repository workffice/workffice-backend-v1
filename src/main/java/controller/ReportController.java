package controller;

import backoffice.application.dto.office_branch.OfficeBranchError;
import controller.response.DataResponse;
import io.vavr.control.Either;
import report.application.OfficeBranchReporter;
import shared.application.UseCaseError;

import java.time.Month;
import java.time.Year;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

@RestController
@RequestMapping(value = "/api/office_branch_reports")
public class ReportController extends BaseController {

    ResponseEntity<DataResponse> notFound = ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(notFound(
                    "OFFICE_BRANCH_NOT_FOUND",
                    "There is no office branch with id provided"
            ));
    ResponseEntity<DataResponse> forbidden = ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(forbidden(
                    "OFFICE_BRANCH_FORBIDDEN",
                    "You do not have access to office branch reports"
            ));
    ResponseEntity<DataResponse> invalidId = ResponseEntity
            .badRequest().body(invalid(
                    "INVALID_OFFICE_BRANCH_ID",
                    "The office branch id specified is invalid"
            ));

    @Autowired
    OfficeBranchReporter officeBranchReporter;

    private <T> ResponseEntity<DataResponse> processResponse(Supplier<Either<UseCaseError, List<T>>> getReport) {
        try {
            return getReport.get()
                    .map(report -> ResponseEntity.ok((DataResponse) entityResponse(report)))
                    .getOrElseGet(error -> Match(error).of(
                            Case($(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST), notFound),
                            Case($(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN), forbidden)
                    ));
        } catch (IllegalArgumentException e) {
            return invalidId;
        }
    }

    @GetMapping("/{officeBranchId}/total_amount_per_office/")
    public ResponseEntity<?> getTotalAmountReportPerOffice(
            @PathVariable String officeBranchId,
            @RequestParam String month
    ) {
        return processResponse(() -> officeBranchReporter
                .transactionAmountPerOfficeReport(officeBranchId, Month.valueOf(month)));
    }

    @GetMapping("/{officeBranchId}/total_bookings_per_office/")
    public ResponseEntity<?> getBookingsReport(
            @PathVariable String officeBranchId,
            @RequestParam String month
    ) {
        return processResponse(() -> officeBranchReporter
                .officeBookingsQuantityReport(officeBranchId, Month.valueOf(month)));
    }

    @GetMapping("/{officeBranchId}/total_amount_per_month/")
    public ResponseEntity<?> getTotalAmountPerMonthReport(
            @PathVariable String officeBranchId,
            @RequestParam Integer year
    ) {
        return processResponse(() -> officeBranchReporter
                .transactionAmountReport(officeBranchId, Year.of(year)));
    }
}

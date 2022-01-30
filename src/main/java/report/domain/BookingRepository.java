package report.domain;

import io.vavr.control.Option;
import io.vavr.control.Try;

import java.time.Month;
import java.time.Year;
import java.util.List;

public interface BookingRepository {
    Try<Void> store(Booking booking);

    Option<Booking> findById(String id);

    List<TransactionAmountProjection> transactionAmountReport(String officeBranchId, Year year);

    List<OfficeTransactionAmountProjection> officesTransactionAmountReport(String officeBranchId, Month month);

    List<OfficeBookedProjection> officeBookedReport(String officeBranchId, Month month);
}

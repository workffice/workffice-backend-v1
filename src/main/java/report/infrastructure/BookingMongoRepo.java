package report.infrastructure;

import io.vavr.control.Option;
import io.vavr.control.Try;
import report.domain.Booking;
import report.domain.BookingRepository;
import report.domain.OfficeBookedProjection;
import report.domain.OfficeTransactionAmountProjection;
import report.domain.TransactionAmountProjection;

import java.time.Month;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;

@Repository
public class BookingMongoRepo implements BookingRepository {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Try<Void> store(Booking booking) {
        return Try.run(() -> mongoTemplate.insert(booking));
    }

    @Override
    public Option<Booking> findById(String id) {
        return Option.of(mongoTemplate.findById(id, Booking.class));
    }

    private MatchOperation filterByOfficeBranchAndMonth(String officeBranchId, String month) {
        return match(Criteria
                .where("officeBranchId")
                .is(officeBranchId)
                .and("year").is(Year.now().getValue())
                .and("month").is(month));
    }

    @Override
    public List<TransactionAmountProjection> transactionAmountReport(String officeBranchId, Year year) {
        var filterByOfficeBranch = match(Criteria
                .where("officeBranchId")
                .is(officeBranchId).and("year").is(year.getValue()));
        var groupByMonth = group("month", "year").sum("amount").as("totalAmount");
        var project = project()
                .andExpression("_id.year").as("year")
                .and("_id.month").as("month")
                .andInclude("totalAmount");
        var aggregation = newAggregation(filterByOfficeBranch, groupByMonth, project);
        return mongoTemplate
                .aggregate(aggregation, Booking.class, TransactionAmountProjection.class)
                .getMappedResults()
                .stream()
                .sorted((a, b) -> Month.valueOf(a.getMonth()).compareTo(Month.valueOf(b.getMonth())) * -1)
                .collect(Collectors.toList());
    }

    @Override
    public List<OfficeTransactionAmountProjection> officesTransactionAmountReport(String officeBranchId, Month month) {
        var filterByOfficeBranch = filterByOfficeBranchAndMonth(officeBranchId, month.name());
        var groupByOffice = group("officeId", "month").sum("amount").as("totalAmount");
        var sortByTotalAmount = sort(Sort.by(Sort.Direction.DESC, "totalAmount", "_id.officeId"));
        var project = project()
                .andExpression("_id.officeId")
                .as("officeId")
                .andInclude("month", "totalAmount");
        var aggregation = newAggregation(filterByOfficeBranch, groupByOffice, sortByTotalAmount, project);
        return mongoTemplate
                .aggregate(aggregation, Booking.class, OfficeTransactionAmountProjection.class)
                .getMappedResults();
    }

    @Override
    public List<OfficeBookedProjection> officeBookedReport(String officeBranchId, Month month) {
        var filterByOfficeBranch = filterByOfficeBranchAndMonth(officeBranchId, month.name());
        var groupByOffice = group("officeId", "month")
                .count()
                .as("totalBookings");
        var sortByTotalAmount = sort(Sort.by(Sort.Direction.DESC, "totalBookings", "_id.officeId"));
        var project = project()
                .andExpression("_id.officeId")
                .as("officeId")
                .andInclude("month", "totalBookings");
        var aggregation = newAggregation(filterByOfficeBranch, groupByOffice, sortByTotalAmount, project);
        return mongoTemplate
                .aggregate(aggregation, Booking.class, OfficeBookedProjection.class)
                .getMappedResults();
    }
}

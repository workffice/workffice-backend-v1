package booking.infrastructure.repositories;

import booking.domain.booking.Booking;
import booking.domain.booking.BookingId;
import booking.domain.booking.BookingRepository;
import booking.domain.office.Office;
import io.vavr.Function3;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import org.hibernate.query.criteria.internal.OrderImpl;
import org.springframework.stereotype.Repository;

@Repository
public class BookingMySQLRepository
        extends BookingJPARepo<Booking, BookingId> implements BookingRepository {

    @Override
    public Try<Void> store(Booking booking) {
        return save(booking);
    }

    @Override
    public Try<Void> update(Booking booking) {
        return merge(booking);
    }

    @Override
    public Option<Booking> findById(BookingId id) {
        Function3<
                CriteriaQuery<Booking>,
                Root<Booking>,
                CriteriaBuilder,
                CriteriaQuery<Booking>
                > addConstraints =
                (query, table, builder) -> query.where(builder.equal(table.get("id"), id));
        Consumer<Root<Booking>> join = table -> {
            table.fetch("office");
            table.fetch("paymentInformation", JoinType.LEFT);
        };
        return findOne(addConstraints, join, getEntityClass());
    }

    private List<Booking> findBookings(Function3<
            CriteriaQuery<Booking>,
            Root<Booking>,
            CriteriaBuilder,
            CriteriaQuery<Booking>
            > criteria, Integer offset, Integer limit, boolean expandOfficeInformation
    ) {
        var entityManager = entityManagerFactory.createEntityManager();
        Consumer<Root<Booking>> join = table -> {
            table.fetch("paymentInformation", JoinType.LEFT);
            if (expandOfficeInformation)
                table.fetch("office");
        };
        List<Booking> bookings;
        if (offset != null && limit != null)
            bookings = findAll(entityManager, criteria, join, offset, limit, getEntityClass());
        else
            bookings = findAll(entityManager, criteria, join, getEntityClass());
        entityManager.close();
        return bookings;
    }

    @Override
    public List<Booking> find(Office office, LocalDate proposedScheduleDate) {
        Function3<
                CriteriaQuery<Booking>,
                Root<Booking>,
                CriteriaBuilder,
                CriteriaQuery<Booking>
                > addConstraints = (query, table, builder) -> {
            var isRelatedWithOffice = builder.equal(table.get("office"), office);
            var isScheduledAtProposedDate = builder
                    .equal(table.get("scheduleTime").get("scheduleDate"), proposedScheduleDate);
            return query.where(builder.and(isRelatedWithOffice, isScheduledAtProposedDate));
        };
        return findBookings(addConstraints, null, null, false);
    }

    @Override
    public List<Booking> find(
            String renterEmail,
            boolean fetchCurrentBookings,
            LocalDate currentDate,
            Integer offset,
            Integer limit
    ) {
        Function3<
                CriteriaQuery<Booking>,
                Root<Booking>,
                CriteriaBuilder,
                CriteriaQuery<Booking>
                > addConstraints =
                (query, table, builder) -> {
                    Path<LocalDate> scheduleDateColumn = table.get("scheduleTime").get("scheduleDate");
                    var currentBookings = builder.greaterThanOrEqualTo(scheduleDateColumn, currentDate);
                    var pastBookings = builder.lessThan(scheduleDateColumn, currentDate);
                    var renterEmailBookings = builder.equal(table.get("renterEmail"), renterEmail);
                    return query
                            .where(fetchCurrentBookings
                                    ? builder.and(renterEmailBookings, currentBookings)
                                    : builder.and(renterEmailBookings, pastBookings))
                            .orderBy(fetchCurrentBookings
                                    ? new OrderImpl(table.get("scheduleTime").get("startTime"), true)
                                    : new OrderImpl(table.get("scheduleTime").get("startTime"), false)
                            );
                };
        return findBookings(addConstraints, offset, limit, true);
    }

    @Override
    public Long count(String renterEmail, boolean fetchCurrentBookings, LocalDate currentDate) {
        var entityManager = entityManagerFactory.createEntityManager();
        var criteriaBuilder = entityManager.getCriteriaBuilder();
        var query = criteriaBuilder.createQuery(Long.class);
        var bookingTable = query.from(getEntityClass());
        query.select(criteriaBuilder.count(bookingTable));

        Path<LocalDate> scheduleDateColumn = bookingTable.get("scheduleTime").get("scheduleDate");
        var currentBookings = criteriaBuilder.greaterThanOrEqualTo(scheduleDateColumn, currentDate);
        var pastBookings = criteriaBuilder.lessThan(scheduleDateColumn, currentDate);
        var renterEmailBookings = criteriaBuilder.equal(bookingTable.get("renterEmail"), renterEmail);
        query.where(fetchCurrentBookings
                ? criteriaBuilder.and(renterEmailBookings, currentBookings)
                : criteriaBuilder.and(renterEmailBookings, pastBookings));
        var result = entityManager.createQuery(query).getSingleResult();
        entityManager.close();
        return result;
    }

    @Override
    public boolean exists(String renterEmail, Office office) {
        Function3<CriteriaQuery<Booking>, Root<Booking>, CriteriaBuilder, CriteriaQuery<Booking>> addConstraints
                = (query, table, builder) -> {
            var equalsToEmail = builder.equal(table.get("renterEmail"), renterEmail);
            var equalsToOffice = builder.equal(table.get("office"), office);
            return query.where(builder.and(equalsToEmail, equalsToOffice));
        };
        Consumer<Root<Booking>> join = table -> { };
        return findOne(addConstraints, join, getEntityClass()).isDefined();
    }

    @Override
    public Class<Booking> getEntityClass() {
        return Booking.class;
    }
}

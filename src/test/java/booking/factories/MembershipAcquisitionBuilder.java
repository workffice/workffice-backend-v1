package booking.factories;

import booking.domain.booking.Booking;
import booking.domain.membership_acquisiton.MembershipAcquisition;
import booking.domain.membership_acquisiton.MembershipAcquisitionId;
import com.github.javafaker.Faker;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.time.DayOfWeek;
import java.time.Month;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MembershipAcquisitionBuilder {
    Faker faker = Faker.instance();
    private MembershipAcquisitionId id = new MembershipAcquisitionId();
    private String membershipId = UUID.randomUUID().toString();
    private Month month = Month.SEPTEMBER;
    private String buyerEmail = faker.internet().emailAddress();
    private Integer price = faker.number().numberBetween(100, 10000);
    private List<Booking> bookings = ImmutableList.of();
    private Set<DayOfWeek> accessDays = ImmutableSet.of();


    public MembershipAcquisitionBuilder withId(MembershipAcquisitionId id) {
        this.id = id;
        return this;
    }

    public MembershipAcquisitionBuilder withMonth(Month month) {
        this.month = month;
        return this;
    }

    public MembershipAcquisitionBuilder withPrice(Integer price) {
        this.price = price;
        return this;
    }

    public MembershipAcquisitionBuilder withAccessDays(Set<DayOfWeek> accessDays) {
        this.accessDays = accessDays;
        return this;
    }

    public MembershipAcquisitionBuilder withBuyerEmail(String email) {
        this.buyerEmail = email;
        return this;
    }

    public MembershipAcquisition build() {
        return MembershipAcquisition.create(
                id,
                membershipId,
                buyerEmail,
                price,
                month,
                accessDays
        );
    }
}

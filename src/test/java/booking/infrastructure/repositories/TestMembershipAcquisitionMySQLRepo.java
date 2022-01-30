package booking.infrastructure.repositories;

import booking.domain.booking.PaymentInformation;
import booking.domain.membership_acquisiton.MembershipAcquisition;
import booking.domain.membership_acquisiton.MembershipAcquisitionId;
import booking.factories.MembershipAcquisitionBuilder;
import com.google.common.collect.ImmutableSet;
import io.vavr.control.Option;
import server.WorkfficeApplication;

import java.time.DayOfWeek;
import java.time.Month;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = {WorkfficeApplication.class})
public class TestMembershipAcquisitionMySQLRepo {
    @Autowired
    MembershipAcquisitionMySQLRepo membershipAcquisitionRepo;

    @Test
    void itShouldStoreMembershipAcquisition() {
        var id = new MembershipAcquisitionId();
        var membershipAcquisition = MembershipAcquisition.create(
                id,
                "1",
                "john@wick.com",
                100,
                Month.APRIL,
                ImmutableSet.of(DayOfWeek.MONDAY)
        );

        membershipAcquisitionRepo.store(membershipAcquisition);

        Option<MembershipAcquisition> maybeMembershipAcquisition = membershipAcquisitionRepo.findById(id);
        assertThat(maybeMembershipAcquisition.isDefined()).isTrue();
        assertThat(maybeMembershipAcquisition.get()).isEqualTo(membershipAcquisition);
    }

    @Test
    void itShouldUpdateMembershipAcquisitionInformation() {
        var id = new MembershipAcquisitionId();
        var membershipAcquisition = MembershipAcquisition.create(
                id,
                "1",
                "john@wick.com",
                100,
                Month.APRIL,
                ImmutableSet.of(DayOfWeek.MONDAY)
        );
        membershipAcquisitionRepo.store(membershipAcquisition);

        var paymentInformation = new PaymentInformation(
                "1",
                100f,
                0f,
                "ARS",
                "visa",
                "credit_card"
        );
        membershipAcquisition.buy(paymentInformation);
        membershipAcquisitionRepo.update(membershipAcquisition).get();

        MembershipAcquisition membershipAcquisitionUpdated = membershipAcquisitionRepo.findById(id).get();
        assertThat(membershipAcquisitionUpdated.isPending()).isFalse();
        assertThat(membershipAcquisitionUpdated.paymentInformation()).isEqualTo(paymentInformation);
    }

    @Test
    void itShouldReturnAllMembershipAcquisitionsRelatedWithBuyerEmail() {
        var membershipAcquisition1 = new MembershipAcquisitionBuilder()
                .withAccessDays(ImmutableSet.of(DayOfWeek.MONDAY))
                .withBuyerEmail("john@doe.com")
                .build();
        var membershipAcquisition2 = new MembershipAcquisitionBuilder()
                .withAccessDays(ImmutableSet.of(DayOfWeek.FRIDAY))
                .withBuyerEmail("john@doe.com")
                .build();
        var membershipAcquisition3 = new MembershipAcquisitionBuilder()
                .withAccessDays(ImmutableSet.of(DayOfWeek.WEDNESDAY))
                .withBuyerEmail("john@doe.com")
                .build();
        membershipAcquisitionRepo.store(membershipAcquisition1);
        membershipAcquisitionRepo.store(membershipAcquisition2);
        membershipAcquisitionRepo.store(membershipAcquisition3);

        var membershipAcquisitions = membershipAcquisitionRepo
                .find("john@doe.com");

        assertThat(membershipAcquisitions).containsExactlyInAnyOrder(
                membershipAcquisition1,
                membershipAcquisition2,
                membershipAcquisition3
        );
    }
}

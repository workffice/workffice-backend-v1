package booking.domain.membership_acquisiton;

import booking.application.dto.membership_acquisition.MembershipAcquisitionPreference;
import booking.application.dto.membership_acquisition.MembershipAcquisitionResponse;
import booking.domain.booking.Booking;
import booking.domain.booking.PaymentInformation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import static booking.domain.membership_acquisiton.MembershipAcquisitionStatus.BOUGHT;
import static booking.domain.membership_acquisiton.MembershipAcquisitionStatus.PENDING;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"bookings", "paymentInformation"})
@Table(name = "membership_acquisitions")
@Entity
public class MembershipAcquisition {
    @EmbeddedId
    private MembershipAcquisitionId id;
    @Column
    private String membershipId;
    @Column
    private Month month;
    @Column
    private String buyerEmail;
    @Column
    private Integer price;
    @Enumerated(EnumType.STRING)
    private MembershipAcquisitionStatus status;
    @OneToMany
    @JoinColumn(name = "membership_id")
    private List<Booking> bookings;
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "membership_access_days", joinColumns = @JoinColumn(name = "membership_id"))
    private Set<DayOfWeek> accessDays;
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private PaymentInformation paymentInformation;

    public static MembershipAcquisition create(
            MembershipAcquisitionId id,
            String membershipId,
            String buyerEmail,
            Integer price,
            Month month,
            Set<DayOfWeek> accessDays
    ) {
        return new MembershipAcquisition(
                id,
                membershipId,
                month,
                buyerEmail,
                price,
                PENDING,
                new ArrayList<>(),
                accessDays,
                null
        );
    }

    public MembershipAcquisitionId id() { return id; }

    public String buyerEmail() { return buyerEmail; }

    public MembershipAcquisitionPreference createPaymentPreference() {
        return MembershipAcquisitionPreference.of(
                id.toString(),
                "",
                buyerEmail,
                (float) price,
                "Compra de membresia"
        );
    }

    public MembershipAcquisitionResponse toResponse() {
        var paymentInformation = this.paymentInformation == null ? null :
                MembershipAcquisitionResponse.PaymentInformation.of(
                        this.paymentInformation.getId(),
                        this.paymentInformation.getExternalId(),
                        this.paymentInformation.getTransactionAmount(),
                        this.paymentInformation.getProviderFee(),
                        this.paymentInformation.getCurrency(),
                        this.paymentInformation.getPaymentMethodId(),
                        this.paymentInformation.getPaymentTypeId()
                );
        return MembershipAcquisitionResponse.of(
                id.toString(),
                membershipId,
                month.name(),
                buyerEmail,
                price,
                status.name(),
                accessDays.stream().map(DayOfWeek::name).collect(Collectors.toSet()),
                paymentInformation
        );
    }

    public boolean isPending() { return status.equals(PENDING); }

    public boolean isBought() { return status.equals(BOUGHT); }

    public PaymentInformation paymentInformation() { return paymentInformation; }

    public void buy(PaymentInformation paymentInformation) {
        this.status = BOUGHT;
        this.paymentInformation = paymentInformation;
    }

    public boolean isActive(Month currentMonth, LocalDate proposedScheduleDate) {
        return isBought() && month.equals(currentMonth) && accessDays.contains(proposedScheduleDate.getDayOfWeek());
    }
}

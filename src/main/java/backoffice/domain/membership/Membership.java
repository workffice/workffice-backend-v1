package backoffice.domain.membership;

import backoffice.application.dto.membership.MembershipInformation;
import backoffice.application.dto.membership.MembershipResponse;
import backoffice.domain.office_branch.OfficeBranch;
import com.google.common.collect.Sets;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "memberships")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "name", "description", "deleted", "accessDays", "pricePerMonth"})
public class Membership {
    @EmbeddedId
    private MembershipId id;
    @Column
    private String name;
    @Column
    private String description;
    @Column
    private LocalDate created;
    @Column
    private LocalDate changed;
    @Column
    private boolean deleted;
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "membership_access_days", joinColumns = @JoinColumn(name = "membership_id"))
    private Set<DayOfWeek> accessDays;
    @Column
    private Integer pricePerMonth;
    @ManyToOne(fetch = FetchType.LAZY)
    private OfficeBranch officeBranch;

    public static Membership createNew(
            MembershipId id,
            String name,
            String description,
            Integer pricePerMonth,
            OfficeBranch officeBranch
    ) {
        var created = LocalDate.now(Clock.systemUTC());
        var changed = LocalDate.now(Clock.systemUTC());
        boolean deleted = false;
        Set<DayOfWeek> accessDays = Sets.newHashSet();
        return new Membership(
                id,
                name,
                description,
                created,
                changed,
                deleted,
                accessDays,
                pricePerMonth,
                officeBranch
        );
    }

    public void configAccessDays(Set<DayOfWeek> newAccessDays) {
        this.accessDays.addAll(newAccessDays);
    }

    public MembershipResponse toResponse() {
        return MembershipResponse.of(
                id.toString(),
                name,
                description,
                accessDays.stream().map(Enum::name).collect(Collectors.toSet()),
                pricePerMonth
        );
    }

    public void delete() {
        this.deleted = true;
    }

    public MembershipId id() { return id; }

    public boolean isDeleted() {
        return deleted;
    }

    public OfficeBranch officeBranch() {
        return officeBranch;
    }

    public Membership update(MembershipInformation info) {
        this.name = info.getName() != null ? info.getName() : this.name;
        this.description = info.getDescription() != null ? info.getDescription() : this.description;
        this.pricePerMonth = info.getPricePerMonth() != null ? info.getPricePerMonth() : this.pricePerMonth;
        Set<DayOfWeek> newAccessDays = info.getAccessDays() != null ? info.getAccessDays()
                .stream()
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toSet()) : Collections.emptySet();
        this.accessDays = newAccessDays.isEmpty() ? this.accessDays : newAccessDays;
        this.changed = LocalDate.now(Clock.systemUTC());
        return this;
    }
}

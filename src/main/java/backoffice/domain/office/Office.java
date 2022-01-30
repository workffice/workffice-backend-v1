package backoffice.domain.office;

import backoffice.application.dto.office.OfficeResponse;
import backoffice.application.dto.office.OfficeUpdateInformation;
import backoffice.domain.equipment.Equipment;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.service.Service;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "offices")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class Office {
    @EmbeddedId
    private OfficeId id;
    @Column(nullable = false)
    private String name;
    @Column(length = 300)
    private String description;
    @Column(nullable = false)
    private Integer capacity;
    @Column(nullable = false)
    private Integer price;
    @Embedded
    private Image image;
    @Enumerated(EnumType.STRING)
    private Privacy privacy;
    @Embedded
    private Tables tables;
    @ManyToOne(fetch = FetchType.LAZY)
    private OfficeBranch officeBranch;
    @Column
    private LocalDate deletedAt;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "office_services",
            joinColumns = {@JoinColumn(name = "office_id")},
            inverseJoinColumns = {@JoinColumn(name = "service_id")}
    )
    private Set<Service> services;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "office_equipments",
            joinColumns = {@JoinColumn(name = "office_id")},
            inverseJoinColumns = {@JoinColumn(name = "equipment_id")}
    )
    private Set<Equipment> equipments;

    public static Try<Office> create(
            OfficeId id,
            String name,
            String description,
            Integer capacity,
            Integer price,
            Image image,
            Privacy privacy,
            OfficeBranch officeBranch,
            Tables tables
    ) {
        if (privacy.equals(Privacy.SHARED) && !tables.hasAllInformation())
            return Try.failure(new IllegalArgumentException("Shared offices must have tables defined"));
        var office = new Office(
                id,
                name,
                description,
                capacity,
                price,
                image.hasEmptyInformation() ? null : image,
                privacy,
                tables.hasEmptyInformation() ? null : tables,
                officeBranch,
                null,
                new HashSet<>(),
                new HashSet<>()
        );
        return Try.success(office);
    }

    public void delete(LocalDate date) {
        deletedAt = date;
    }

    public boolean isDeleted(LocalDate today) {
        if (deletedAt == null)
            return false;
        return today.isAfter(deletedAt);
    }

    public OfficeId id() { return id; }

    private Option<Image> image() { return Option.of(image); }

    private Option<Tables> tables() { return Option.of(tables); }

    public OfficeBranch officeBranch() { return officeBranch; }

    public Try<Office> update(OfficeUpdateInformation info) {
        boolean willBeShared = info.privacy().map(privacy -> privacy.equals(Privacy.SHARED.name())).orElse(false);
        if (willBeShared && (info.tablesQuantity().isEmpty() || info.capacityPerTable().isEmpty()))
            return Try.failure(new IllegalArgumentException("Shared offices must have tables defined"));
        this.name = info.name().isPresent() ? info.name().get() : this.name;
        this.description = info.description().isPresent() ? info.description().get() : this.description;
        this.capacity = info.capacity().isPresent() ? info.capacity().get() : this.capacity;
        this.price = info.price().isPresent() ? info.price().get() : this.price;
        this.privacy = info.privacy().isPresent() ? Privacy.valueOf(info.privacy().get()) : this.privacy;
        this.image = info.imageUrl().isPresent() ? new Image(info.imageUrl().get()) : this.image;
        this.tables = Tables.create(
                info.tablesQuantity().isPresent() ? info.tablesQuantity().get() : tables.quantity(),
                info.capacityPerTable().isPresent() ? info.capacityPerTable().get() : tables.capacityPerTable()
        );
        return Try.success(this);
    }

    public void addEquipments(Set<Equipment> equipments) {
        this.equipments.addAll(equipments);
    }

    public void setEquipments(Set<Equipment> equipments) {
        this.equipments = equipments;
    }

    public void addServices(Set<Service> services) {
        this.services.addAll(services);
    }

    public void setServices(Set<Service> services) {
        this.services = services;
    }

    public OfficeResponse toResponse() {
        var tableResponse = OfficeResponse.TableResponse.of(
                tables().map(Tables::quantity).getOrNull(),
                tables().map(Tables::capacityPerTable).getOrNull()
        );
        var serviceResponses = services
                .stream()
                .map(Service::toResponse)
                .collect(Collectors.toSet());
        var equipmentResponses = equipments
                .stream()
                .map(Equipment::toResponse)
                .collect(Collectors.toSet());
        return OfficeResponse.of(
                id.toString(),
                name,
                description,
                capacity,
                price,
                image().map(Image::url).getOrNull(),
                privacy.toString(),
                deletedAt,
                tableResponse,
                serviceResponses,
                equipmentResponses
        );
    }

    public OfficeCreatedEvent officeCreatedEvent() {
        return OfficeCreatedEvent.of(
                id.toString(),
                officeBranch.id().toString(),
                name,
                privacy.toString(),
                price,
                capacity,
                tables().map(Tables::quantity).getOrElse(0),
                tables().map(Tables::capacityPerTable).getOrElse(0)
        );
    }

    public OfficeUpdatedEvent officeUpdatedEvent() {
        return OfficeUpdatedEvent.of(
                id.toString(),
                officeBranch.id().toString(),
                name,
                privacy.toString(),
                price,
                capacity,
                tables().map(Tables::quantity).getOrElse(0),
                tables().map(Tables::capacityPerTable).getOrElse(0)
        );
    }

    public OfficeDeletedEvent officeDeletedEvent() {
        return OfficeDeletedEvent.of(
                officeBranch.id().toString(),
                this.id.toString()
        );
    }
}

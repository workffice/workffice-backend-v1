package backoffice.domain.office_branch;

import backoffice.application.dto.office_branch.OfficeBranchResponse;
import backoffice.application.dto.office_branch.OfficeBranchUpdateInformation;
import backoffice.domain.office_holder.OfficeHolder;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;

@Entity
@Table(name = "office_branches")
@NoArgsConstructor
public class OfficeBranch {
    @EmbeddedId
    private OfficeBranchId id;
    @Column
    private String name;
    @Column(length = 300)
    private String description;
    @Column
    private String phone;
    @Column
    private LocalDate created;
    @Column
    private boolean deleted;
    @OneToMany(cascade = {PERSIST, MERGE}, fetch = FetchType.LAZY)
    @JoinColumn(name = "officeBranchId")
    private List<Image> images;
    @OneToOne(cascade = {PERSIST, MERGE}, fetch = FetchType.LAZY)
    private Location location;
    @ManyToOne(fetch = FetchType.LAZY)
    private OfficeHolder owner;

    public OfficeBranch(
            OfficeBranchId id,
            OfficeHolder owner,
            String name,
            String desc,
            String phone,
            LocalDate created,
            List<Image> images,
            Location location
    ) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.description = desc;
        this.phone = phone;
        this.created = created;
        this.images = images;
        this.location = location;
        this.deleted = false;
    }

    public static OfficeBranch createNew(
            OfficeBranchId id,
            OfficeHolder owner,
            String name,
            String desc,
            String phone,
            Location location
    ) {
        LocalDate now = LocalDate.now(Clock.systemUTC());
        List<Image> emptyImages = new ArrayList<>();
        return new OfficeBranch(
                id,
                owner,
                name,
                desc,
                phone,
                now,
                emptyImages,
                location
        );
    }

    public static OfficeBranch fromDTO(OfficeBranchResponse dto) {
        var images = dto
                .getImages()
                .stream()
                .map(i -> new Image(i.getUrl())).collect(Collectors.toList());
        var location = new Location(
                dto.getLocation().getProvince(),
                dto.getLocation().getCity(),
                dto.getLocation().getStreet(),
                dto.getLocation().getZipCode()
        );
        return new OfficeBranch(
                OfficeBranchId.fromString(dto.getId()),
                null, // DTO does not provide owner information
                dto.getName(),
                dto.getDescription(),
                dto.getPhone(),
                dto.getCreated(),
                images,
                location
        );
    }

    public OfficeBranch update(OfficeBranchUpdateInformation info) {
        this.name = info.name().isPresent() ? info.name().get() : this.name;
        this.description = info.description().isPresent() ? info.description().get() : this.description;
        this.phone = info.phone().isPresent() ? info.phone().get() : this.phone;
        this.location = new Location(
                this.location.id(),
                info.province().isPresent() ? info.province().get() : this.location.province(),
                info.city().isPresent() ? info.city().get() : this.location.city(),
                info.street().isPresent() ? info.street().get() : this.location.street(),
                info.zipCode().isPresent() ? info.zipCode().get() : this.location.zipCode()
        );
        if (info.imageUrls().isPresent()) {
            var newImageUrls = info.imageUrls().get();
            var imagesAlreadyStored = this.images
                    .stream()
                    .filter(image -> newImageUrls.contains(image.url()))
                    .collect(Collectors.toList());
            var newImages = newImageUrls
                    .stream()
                    .map(Image::new)
                    .filter(image -> !imagesAlreadyStored.contains(image))
                    .collect(Collectors.toList());
            newImages.addAll(imagesAlreadyStored);
            this.images = newImages;
        }
        return this;
    }

    public OfficeBranch delete() {
        deleted = true;
        return this;
    }

    public void addImages(List<Image> images) {
        this.images.addAll(images);
    }

    public OfficeBranchId id() { return id; }

    public String name() { return name; }

    public String description() { return description; }

    public String phone() { return phone; }

    public LocalDate created() { return created; }

    public List<Image> images() { return images; }

    public Location location() { return location; }

    public OfficeHolder owner() { return owner; }

    public boolean isDeleted() { return deleted; }

    public OfficeBranchResponse toResponse() {
        List<OfficeBranchResponse.Image> images = this.images
                .stream()
                .map(image -> new OfficeBranchResponse.Image(image.url()))
                .collect(Collectors.toList());
        OfficeBranchResponse.Location location = new OfficeBranchResponse.Location(
                this.location.province(),
                this.location.city(),
                this.location.street(),
                this.location.zipCode()
        );
        return OfficeBranchResponse.of(
                id.toString(),
                name,
                description,
                phone,
                created,
                images,
                location
        );
    }

    public OfficeBranchCreatedEvent officeBranchCreatedEvent() {
        return OfficeBranchCreatedEvent.of(
                id.toString(),
                owner.id().toString(),
                name,
                location.province(),
                location.city(),
                location().street(),
                phone,
                images.stream().map(Image::url).collect(Collectors.toList())
        );
    }

    public OfficeBranchUpdatedEvent officeBranchUpdatedEvent() {
        return OfficeBranchUpdatedEvent.of(
                id.toString(),
                name,
                location.province(),
                location.city(),
                location.street(),
                phone,
                images.stream().map(Image::url).collect(Collectors.toList())
        );
    }

    public OfficeBranchDeletedEvent officeBranchDeletedEvent() {
        return OfficeBranchDeletedEvent.of(id.toString());
    }
}

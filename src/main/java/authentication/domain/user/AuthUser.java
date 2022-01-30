package authentication.domain.user;

import authentication.application.dto.user.AuthUserResponse;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

@Entity
@Table(name = "auth_users")
@NoArgsConstructor
@AllArgsConstructor
public class AuthUser {
    
    @EmbeddedId
    private AuthUserId id;
    @Column(unique = true)
    private String email;
    @Column
    private String password;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column
    private String name;
    @Column
    private String lastname;
    @Column
    private String address;
    @Column
    private String bio;
    @Column
    private String profileImage;

    private AuthUser(AuthUserId id, String email, String password, Status status) {
        this.id       = id;
        this.email    = email;
        this.password = password;
        this.status   = status;
    }

    public static AuthUser createNew(AuthUserId id, String email, String password) {
        return new AuthUser(id, email, password, Status.PENDING);
    }
    
    public String email() {
        return email;
    }
    
    public String password() {
        return password;
    }
    
    public AuthUserId id() {
        return id;
    }
    
    public boolean isActive() {
        return status.equals(Status.ACTIVE);
    }
    
    public void activate() {
        this.status = Status.ACTIVE;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public AuthUser update(
            String name,
            String lastname,
            String address,
            String bio,
            String profileImage
    ) {
        this.name     = name;
        this.lastname = lastname;
        this.address  = address;
        this.bio      = bio;
        this.profileImage = profileImage;
        return this;
    }

    public AuthUserResponse toResponse(String userType) {
        return AuthUserResponse.of(
                id.toString(),
                email,
                name,
                lastname,
                address,
                bio,
                userType,
                profileImage
        );
    }

    public AuthUserResponse toResponse() {
        return AuthUserResponse.of(
                id.toString(),
                email,
                name,
                lastname,
                address,
                bio,
                null,
                profileImage
        );
    }
}

package son.suck.muzik.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStatus {

    @Id
    @Column(name = "user_id")
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private Users user;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserOnlineStatus status;

    @Builder
    public UserStatus(Users user, UserOnlineStatus status) {
        this.user = user;
        this.status = (status != null) ? status : UserOnlineStatus.OFFLINE;
    }

    public void initUser(Users user) {
        this.user = user;
    }

    public void updateStatus(UserOnlineStatus status) {
        this.status = status;
    }
}

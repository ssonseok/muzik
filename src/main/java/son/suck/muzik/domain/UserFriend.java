package son.suck.muzik.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFriend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "friend_ship_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_user_id", nullable = false)
    private Users currentUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_user_id", nullable = false)
    private Users friendUser;

    @Column(nullable = false, length = 20)
    private String status; // REQUESTED(친구 요청 상태), ACCEPTED(수락 완료 상태)

    @Builder
    public UserFriend(Users currentUser, Users friendUser, String status) {
        this.currentUser = currentUser;
        this.friendUser = friendUser;
        this.status = status;
    }

    // 친구 요청을 수락했을 때 상태를 변경하는 메서드
    public void acceptFriend() {
        this.status = "ACCEPTED";
    }
    //
    public void updateStatus(String status) {
        this.status = status;
    }
}
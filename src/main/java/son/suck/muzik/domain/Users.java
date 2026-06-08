package son.suck.muzik.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String loginId;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    private UserStatus userStatus;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameParticipant> gameParticipants = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<GameHistoryResult> gameHistoryResults = new ArrayList<>();

    @OneToMany(mappedBy = "currentUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserFriend> friends = new ArrayList<>();

    @Builder
    public Users(String loginId, String password, String nickname) {
        this.loginId = loginId;
        this.password = password;
        this.nickname = nickname;
    }

    // 연관관계 편의 메서드
    public void setUserStatus(UserStatus userStatus) {
        this.userStatus = userStatus;
        // 상대방 객체(UserStatus)에도 나 자신(this)을 연결해 줌
        if (userStatus != null && userStatus.getUser() != this) {
            userStatus.initUser(this);
        }
    }
}

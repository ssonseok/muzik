package son.suck.muzik.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
public class GameParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participant_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private GameRoom gameRoom;

    @Column(nullable = false)
    private boolean isHost; // 방장 여부 (true면 방장, false면 일반 참여자)

    @Column(nullable = false)
    private int currentScore; // 게임 중 실시간 획득 점수 (웹소켓 스코어보드용)

    @Builder
    public GameParticipant(Users user, GameRoom gameRoom, boolean isHost) {
        this.user = user;
        this.gameRoom = gameRoom;
        this.isHost = isHost;
        this.currentScore = 0;
    }

    public void addScore(int score) {
        this.currentScore += score;
    }

    // 원래 방장이 나가서 방장이 위임되거나 할 때 사용할 방장 권한 변경 메서드
    public void updateHost(boolean isHost) {
        this.isHost = isHost;
    }

    // 다음 판 시작할 때 점수 초기화용 메서드
    public void resetScore() {
        this.currentScore = 0;
    }
}

package son.suck.muzik.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameHistoryResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_result_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_history_id", nullable = false)
    private GameHistory gameHistory;

    @Column(nullable = false)
    private int finalScore;

    @Column(nullable = false)
    private int ranking;

    @Builder
    public GameHistoryResult(Users user, GameHistory gameHistory, int finalScore, int ranking) {
        this.user = user;
        this.gameHistory = gameHistory;
        this.finalScore = finalScore;
        this.ranking = ranking;
    }
}

package son.suck.muzik.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_history_id")
    private Long id;

    @Column(nullable = false, length = 30)
    private String playGenre;

    @Column(nullable = false)
    private LocalDateTime playedAt;

    @OneToMany(mappedBy = "gameHistory", cascade = CascadeType.ALL)
    private List<GameHistoryResult> results = new ArrayList<>();

    @Builder
    public GameHistory(String playGenre) {
        this.playGenre = playGenre;
        this.playedAt = LocalDateTime.now();
    }
}

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
public class GameRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String roomName;

    @Column(nullable = false)
    private int maxPlayers; // 최대 인원 (예: 8명)

    @Column(nullable = false, length = 20)
    private String roomStatus;

    @Column(length = 50)
    private String password;

    @Column(nullable = false, length = 30)
    private String genre;

    @Column(nullable = false)
    private int startYear;

    @Column(nullable = false)
    private int endYear;

    @Column(nullable = false)
    private int musicCount;

    @OneToMany(mappedBy = "gameRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameParticipant> participants = new ArrayList<>();

    @Builder
    public GameRoom(String roomName, int maxPlayers, String roomStatus, String password,
                    String genre, int startYear, int endYear, int musicCount) {
        this.roomName = roomName;
        this.maxPlayers = maxPlayers;
        this.roomStatus = roomStatus;
        this.password = password;
        this.genre = genre;
        this.startYear = startYear;
        this.endYear = endYear;
        this.musicCount = musicCount;
    }

    public void updateStatus(String roomStatus) {
        this.roomStatus = roomStatus;
    }
}

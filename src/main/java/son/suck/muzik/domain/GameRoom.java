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
    private String roomStatus; //게임 시작여부(waiting,playing)

    @Column(length = 50)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoomType roomType = RoomType.muzik;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private GamePhase gamePhase;

    @Column(nullable = false)
    private int roundNo = 0;

    @Column(length = 30)
    private String genre;

    private Integer startYear;

    private Integer endYear;

    private Integer musicCount;

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
        this.roomType = RoomType.muzik;
        this.gamePhase = GamePhase.WAITING;
    }

    @Builder(builderClassName = "MiniGameRoomBuilder", builderMethodName = "miniGameRoomBuilder")
    public GameRoom(String roomName, int maxPlayers, String roomStatus, String password, RoomType roomType) {
        this.roomName = roomName;
        this.maxPlayers = maxPlayers;
        this.roomStatus = roomStatus;
        this.password = password;
        this.roomType = roomType;
        this.gamePhase = GamePhase.WAITING;
        this.roundNo = 0;
    }

    public void updateStatus(String roomStatus) {
        this.roomStatus = roomStatus;
    }

    public void updatePhase(GamePhase gamePhase) {
        this.gamePhase = gamePhase;
    }

    public void incrementRound() {
        this.roundNo++;
    }
}

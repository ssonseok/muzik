package son.suck.muzik.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import son.suck.muzik.domain.GameRoom;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
public class GameRoomDetailResponse {

    private Long roomId;
    private String roomName;
    private int maxPlayers;
    private String roomStatus;
    private String genre;
    private int startYear;
    private int endYear;
    private int musicCount;
    private List<ParticipantDto> participants;

    public GameRoomDetailResponse(GameRoom room) {
        this.roomId = room.getId();
        this.roomName = room.getRoomName();
        this.maxPlayers = room.getMaxPlayers();
        this.roomStatus = room.getRoomStatus();
        this.genre = room.getGenre();
        this.startYear = room.getStartYear();
        this.endYear = room.getEndYear();
        this.musicCount = room.getMusicCount();
        this.participants = room.getParticipants().stream()
                .map(ParticipantDto::new)
                .collect(Collectors.toList());
    }
}

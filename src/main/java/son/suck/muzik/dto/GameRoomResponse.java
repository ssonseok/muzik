package son.suck.muzik.dto;

import son.suck.muzik.domain.GameRoom;
import lombok.Getter;

@Getter
public class GameRoomResponse {
    private Long roomId;
    private String roomName;
    private int currentPlayers;
    private int maxPlayers;
    private String roomStatus;
    private String genre;
    private int musicCount;

    public GameRoomResponse(GameRoom gameRoom) {
        this.roomId = gameRoom.getId();
        this.roomName = gameRoom.getRoomName();
        this.currentPlayers = gameRoom.getParticipants().size();
        this.maxPlayers = gameRoom.getMaxPlayers();
        this.roomStatus = gameRoom.getRoomStatus();
        this.genre = gameRoom.getGenre();
        this.musicCount = gameRoom.getMusicCount();
    }
}

package son.suck.muzik.dto;

import lombok.Getter;
import son.suck.muzik.domain.GameRoom;
import son.suck.muzik.domain.RoomType;

@Getter
public class MafiaRoomResponse {

    private Long roomId;
    private String roomName;
    private int currentPlayers;
    private int maxPlayers;
    private String roomStatus;
    private RoomType roomType; // MAFIA

    private int nightTime = 25;//밤
    private int discussionTime = 50;//낮
    private int votingTime = 20;//투표
    private int defenseTime = 15;//최후반론

    public MafiaRoomResponse(GameRoom gameRoom) {
        this.roomId = gameRoom.getId();
        this.roomName = gameRoom.getRoomName();
        this.currentPlayers = gameRoom.getParticipants().size();
        this.maxPlayers = gameRoom.getMaxPlayers();
        this.roomStatus = gameRoom.getRoomStatus();
        this.roomType = gameRoom.getRoomType();
    }
}

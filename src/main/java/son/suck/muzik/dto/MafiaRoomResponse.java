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
    private int discussionTime;
    private int votingTime;
    private int defenseTime = 15;//최후반론

    public MafiaRoomResponse(GameRoom gameRoom) {
        this.roomId = gameRoom.getId();
        this.roomName = gameRoom.getRoomName();
        int players = gameRoom.getParticipants().size();
        this.currentPlayers = players;
        this.maxPlayers = gameRoom.getMaxPlayers();
        this.roomStatus = gameRoom.getRoomStatus();
        this.roomType = gameRoom.getRoomType();
        this.discussionTime = 30 + (players * 5);
        this.votingTime = 15 + (players * 2);
    }
}

package son.suck.muzik.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MafiaCreateRoomRequestDto {

    private Long hostUserId;
    private String roomName;
    private int maxPlayers;
    private String password;

}

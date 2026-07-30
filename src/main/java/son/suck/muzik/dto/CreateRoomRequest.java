package son.suck.muzik.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateRoomRequest {
    private Long hostUserId;
    private String roomName;
    private int maxPlayers;
    private String password;
    private String genre;
    private int musicCount;
    private int startYear;
    private int endYear;
}

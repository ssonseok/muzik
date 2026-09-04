package son.suck.muzik.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MafiaCreateRoomRequestDto {

    private String roomName;
    private int maxPlayers;
    private String password;//테이블은있는데 muzik에서 미구현이라 할까말까 ;;

}

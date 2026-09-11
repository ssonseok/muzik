package son.suck.muzik.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MafiaStartEventDto {
    private Long roomId;
    private int totalPlayers;
}

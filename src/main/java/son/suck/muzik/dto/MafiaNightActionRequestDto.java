package son.suck.muzik.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MafiaNightActionRequestDto {

    private Long userId;
    private Long targetId;
    private ActionType actionType;

    public enum ActionType {
        MAFIA_KILL,
        DOCTOR_HEAL,
        POLICE_INVESTIGATE
    }
}

package son.suck.muzik.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import son.suck.muzik.domain.GamePhase;
import son.suck.muzik.domain.Mafia_Role;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MafiaMyInfoResponse {
    private Long participantId;
    private String nickname;
    private Mafia_Role mafiaRole;
    private boolean isAlive;
    private boolean isHost;
    private GamePhase gamePhase;
}
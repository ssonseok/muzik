package son.suck.muzik.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MafiaParticipantResponse {

    private Long participantId;
    private String nickname;
    private boolean alive;
    private boolean host;
}

package son.suck.muzik.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MafiaGameMessageDto {

    private Long roomId;
    private MessageType messageType;
    private Long senderId;
    private String senderName;
    private String content;

    private String currentPhase;
    private int remainingTime;
    private Long targetId;

    public enum MessageType {
        CHAT,
        SYSTEM_NOTICE,
        PHASE_CHANGE,
        NIGHT_RESULT,
        VOTE_RESULT,
        GAME_OVER
    }
}
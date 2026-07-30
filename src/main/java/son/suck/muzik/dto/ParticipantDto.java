package son.suck.muzik.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import son.suck.muzik.domain.GameParticipant;

@Getter
@NoArgsConstructor
public class ParticipantDto {
    private Long userId;
    private String nickname;
    private boolean isHost;
    private int currentScore;

    public ParticipantDto(GameParticipant participant) {
        this.userId = participant.getUser().getId();
        this.nickname = participant.getUser().getNickname();
        this.isHost = participant.isHost();
        this.currentScore = participant.getCurrentScore();
    }
}
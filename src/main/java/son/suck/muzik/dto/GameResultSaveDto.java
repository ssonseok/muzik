package son.suck.muzik.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GameResultSaveDto {
    private Long userId;
    private int finalScore;

    public GameResultSaveDto(Long userId, int finalScore) {
        this.userId = userId;
        this.finalScore = finalScore;
    }
}

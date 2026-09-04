package son.suck.muzik.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MafiaVoteRequestDto {

    private Long roomId;
    private Long voterId;
    private Long targetId;
}

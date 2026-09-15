package son.suck.muzik.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MafiaExecutionVoteRequestDto {
    private Long roomId;
    //private Long voterId;
    private boolean agree; // true: 찬성(처형), false: 반대(생존)
}

package son.suck.muzik.service;

import son.suck.muzik.dto.MafiaNightActionRequestDto;
import son.suck.muzik.dto.MafiaVoteRequestDto;

public interface MafiaPlayService {
    void processNightAction(MafiaNightActionRequestDto request);
    void calculateNightResult(Long roomId);
    void processVote(MafiaVoteRequestDto request);
    void transitionPhase(Long roomId);
    void checkGameEndCondition(Long roomId);
}

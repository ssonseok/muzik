package son.suck.muzik.service;

import org.springframework.transaction.annotation.Transactional;
import son.suck.muzik.domain.GamePhase;

public interface MafiaPhaseService {
    void startNightPhase(Long roomId, int participantCount);
    void startDayPhase(Long roomId, int participantCount);
    void startVotingPhase(Long roomId, int participantCount);
    void startDefensePhase(Long roomId, int participantCount);
    void stopTimer(Long roomId);
}

package son.suck.muzik.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import son.suck.muzik.domain.GamePhase;
import son.suck.muzik.domain.GameRoom;
import son.suck.muzik.dto.MafiaStartEventDto;
import son.suck.muzik.repository.GameRoomRepository;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MafiaPhaseServiceImpl implements MafiaPhaseService {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> roomTimers = new ConcurrentHashMap<>();

    private final MafiaPlayService mafiaPlayService;
    private final MafiaRoomService mafiaRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleGameStarted(MafiaStartEventDto event) {
        startNightPhase(event.getRoomId(), event.getTotalPlayers());
    }

    private void changePhaseAndBroadcast(Long roomId, GamePhase phase) {
        mafiaRoomService.updateRoomPhase(roomId, phase);

        messagingTemplate.convertAndSend(
                "/sub/room/" + roomId + "/phase",
                Map.of("phase", phase)
        );
    }

    @Override
    public void startNightPhase(Long roomId, int participantCount) {
        changePhaseAndBroadcast(roomId, GamePhase.NIGHT);

        scheduleNextPhase(roomId, 30, () -> {
            mafiaPlayService.calculateNightResult(roomId);
            startDayPhase(roomId, participantCount);
        });
    }

    @Override
    public void startDayPhase(Long roomId, int participantCount) {
        changePhaseAndBroadcast(roomId, GamePhase.DAY);
        int duration = 30 + (participantCount * 5);

        scheduleNextPhase(roomId, duration, () -> {
            startVotingPhase(roomId, participantCount);
        });
    }

    @Override
    public void startVotingPhase(Long roomId, int participantCount) {
        changePhaseAndBroadcast(roomId, GamePhase.VOTE);
        int duration = 15 + (participantCount * 2);

        scheduleNextPhase(roomId, duration, () -> {
            mafiaPlayService.calculateDayResult(roomId);
            startDefensePhase(roomId, participantCount);
        });
    }

    @Override
    public void startDefensePhase(Long roomId, int participantCount) {
        changePhaseAndBroadcast(roomId, GamePhase.DEFENSE);
        int duration = 15;

        scheduleNextPhase(roomId, duration, () -> {
            startNightPhase(roomId, participantCount);
        });
    }

    @Override
    public void stopTimer(Long roomId) {
        ScheduledFuture<?> future = roomTimers.remove(roomId);
        if (future != null) {
            future.cancel(true);
        }
    }

    private void scheduleNextPhase(Long roomId, int seconds, Runnable task) {
        stopTimer(roomId);
        ScheduledFuture<?> future = taskScheduler.schedule(task, Instant.now().plusSeconds(seconds));
        roomTimers.put(roomId, future);
    }
}
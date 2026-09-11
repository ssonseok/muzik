package son.suck.muzik.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import son.suck.muzik.domain.GamePhase;
import son.suck.muzik.domain.GameRoom;
import son.suck.muzik.dto.MafiaStartEventDto;
import son.suck.muzik.repository.GameRoomRepository;

import java.time.Instant;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MafiaPhaseServiceImpl implements MafiaPhaseService {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> roomTimers = new ConcurrentHashMap<>();

    private final MafiaPlayService mafiaPlayService;
    private final MafiaRoomService mafiaRoomService;

    @EventListener
    public void handleGameStarted(MafiaStartEventDto event) {
        startNightPhase(event.getRoomId(), event.getTotalPlayers());
    }

    @Override
    public void startNightPhase(Long roomId, int participantCount) {
        mafiaRoomService.updateRoomPhase(roomId, GamePhase.NIGHT);
        System.out.println("🌙 [방 " + roomId + "] 밤 페이즈 시작! (30초)");

        scheduleNextPhase(roomId, 30, () -> {
            mafiaPlayService.calculateNightResult(roomId);
            System.out.println("🌙 [방 " + roomId + "] 밤 정산 완료 -> 낮 페이즈로 이동");

            startDayPhase(roomId, participantCount);
        });
    }

    @Override
    public void startDayPhase(Long roomId, int participantCount) {
        mafiaRoomService.updateRoomPhase(roomId, GamePhase.DAY);
        int duration = 30 + (participantCount * 5);
        System.out.println("☀️ [방 " + roomId + "] 낮 페이즈(토론) 시작! (" + duration + "초)");

        scheduleNextPhase(roomId, duration, () -> {
            System.out.println("☀️ [방 " + roomId + "] 낮 토론 종료 -> 투표 페이즈로 이동");
            startVotingPhase(roomId, participantCount);
        });
    }

    @Override
    public void startVotingPhase(Long roomId, int participantCount) {
        mafiaRoomService.updateRoomPhase(roomId, GamePhase.VOTE);
        int duration = 15 + (participantCount * 2);
        System.out.println("🗳️ [방 " + roomId + "] 투표 페이즈 시작! (" + duration + "초)");

        scheduleNextPhase(roomId, duration, () -> {
            mafiaPlayService.calculateDayResult(roomId);
            System.out.println("🗳️ [방 " + roomId + "] 투표 마감 -> 최후의 반론으로 이동");
            startDefensePhase(roomId, participantCount);
        });
    }

    @Override
    public void startDefensePhase(Long roomId, int participantCount) {
        mafiaRoomService.updateRoomPhase(roomId, GamePhase.DEFENSE);
        int duration = 15;
        System.out.println("⚖️ [방 " + roomId + "] 최후의 반론 시작! (" + duration + "초)");

        scheduleNextPhase(roomId, duration, () -> {
            System.out.println("⚖️ [방 " + roomId + "] 반론 종료 -> 다시 밤 페이즈로 순환");
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
package son.suck.muzik.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import son.suck.muzik.domain.GameParticipant;
import son.suck.muzik.domain.Mafia_Role;
import son.suck.muzik.dto.MafiaNightActionRequestDto;
import son.suck.muzik.dto.MafiaVoteRequestDto;
import son.suck.muzik.repository.GameParticipantRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MafiaPlayServiceImpl implements MafiaPlayService{

    private final GameParticipantRepository gameParticipantRepository;
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, MafiaNightActionRequestDto>> nightActionStore = new ConcurrentHashMap<>();

    @Override
    public void processNightAction(MafiaNightActionRequestDto request) {
        validateNightAction(request);
        saveNightActionToStore(request);
    }

    /**
     * [헬퍼 1] 밤 행동 유효성 검증
     * - 사망한 유저의 행동 차단
     * - 직업과 액션 타입 매칭 검증 (마피아, 의사, 경찰만 액션 수행 / 시민·군인은 밤에 액션 없음)
     */
    private void validateNightAction(MafiaNightActionRequestDto request) {
        Long roomId = request.getRoomId();
        Long userId = request.getUserId();
        MafiaNightActionRequestDto.ActionType actionType = request.getActionType();

        GameParticipant participant = gameParticipantRepository.findByGameRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 방의 참여자 정보를 찾을 수 없습니다. User ID: " + userId));

        if (!participant.isAlive()) {
            throw new IllegalStateException("사망한 유저는 밤 행동을 수행할 수 없습니다.");
        }

        Mafia_Role role = participant.getMafiaRole();

        boolean isMatched = switch (actionType) {
            case MAFIA_KILL -> role == Mafia_Role.MAFIA;
            case DOCTOR_HEAL -> role == Mafia_Role.DOCTOR;
            case POLICE_INVESTIGATE -> role == Mafia_Role.POLICE;
        };

        if (!isMatched) {
            throw new IllegalArgumentException("해당 직업은 이 밤 행동을 수행할 수 없습니다. 요청 액션: " + actionType + ", 현재 직업: " + role);
        }
    }

    /**
     * [헬퍼 2] 메모리 저장소에 액션 적재
     * - 밤 시간 동안 고민하면서 타겟을 바꿀 수 있으므로 put으로 갱신(덮어쓰기) 허용
     */
    private void saveNightActionToStore(MafiaNightActionRequestDto request) {
        Long roomId = request.getRoomId();
        Long userId = request.getUserId();

        nightActionStore.putIfAbsent(roomId, new ConcurrentHashMap<>());
        ConcurrentHashMap<Long, MafiaNightActionRequestDto> roomActions = nightActionStore.get(roomId);

        roomActions.put(userId, request);
    }

    @Override
    @Transactional
    public void calculateNightResult(Long roomId) {
        ConcurrentHashMap<Long, MafiaNightActionRequestDto> roomActions = nightActionStore.get(roomId);

        if (roomActions == null || roomActions.isEmpty()) {
            nightActionStore.remove(roomId);
            return;
        }

        Long mafiaTargetId = determineMafiaTarget(roomActions);

        Long doctorTargetId = getDoctorTarget(roomActions);

        processPoliceInvestigation(roomActions);

        applyFinalSurvivalResult(roomId, mafiaTargetId, doctorTargetId);

        nightActionStore.remove(roomId);
    }

    /**
     * [헬퍼 1] 마피아 투표 집계 (다수결 및 동률 처리)
     */
    private Long determineMafiaTarget(ConcurrentHashMap<Long, MafiaNightActionRequestDto> roomActions) {
        Map<Long, Integer> mafiaVoteCount = new HashMap<>();

        for (MafiaNightActionRequestDto action : roomActions.values()) {
            if (action.getActionType() == MafiaNightActionRequestDto.ActionType.MAFIA_KILL) {
                mafiaVoteCount.put(action.getTargetId(), mafiaVoteCount.getOrDefault(action.getTargetId(), 0) + 1);
            }
        }

        if (mafiaVoteCount.isEmpty()) {
            return null;
        }


        return Collections.max(mafiaVoteCount.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    /**
     * [헬퍼 2] 의사 힐 타겟 확인
     */
    private Long getDoctorTarget(ConcurrentHashMap<Long, MafiaNightActionRequestDto> roomActions) {
        return roomActions.values().stream()
                .filter(action -> action.getActionType() == MafiaNightActionRequestDto.ActionType.DOCTOR_HEAL)
                .map(MafiaNightActionRequestDto::getTargetId)
                .findFirst()
                .orElse(null);
    }

    /**
     * [헬퍼 3] 경찰 조사 결과 처리
     */
    private void processPoliceInvestigation(ConcurrentHashMap<Long, MafiaNightActionRequestDto> roomActions) {
        roomActions.values().stream()
                .filter(action -> action.getActionType() == MafiaNightActionRequestDto.ActionType.POLICE_INVESTIGATE)
                .forEach(action -> {
                    Long targetId = action.getTargetId();
                    GameParticipant target = gameParticipantRepository.findById(targetId).orElse(null);

                    if (target != null) {
                        boolean isMafia = (target.getMafiaRole() == Mafia_Role.MAFIA);
                        // TODO: 웹소켓을 통해 조사한 경찰 유저(action.getUserId())에게 isMafia 결과 전송
                    }
                });
    }

    /**
     * [헬퍼 4] 최종 생사 판정 (마피아 킬 vs 의사 힐 vs 군인 패시브)
     */
    private void applyFinalSurvivalResult(Long roomId, Long mafiaTargetId, Long doctorTargetId) {
        if (mafiaTargetId == null) {
            return;
        }

        if (mafiaTargetId.equals(doctorTargetId)) {
            return;
        }

        GameParticipant targetParticipant = gameParticipantRepository.findByGameRoomIdAndUserId(roomId, mafiaTargetId)
                .orElseThrow(() -> new IllegalArgumentException("마피아 타겟 참여자 정보를 찾을 수 없습니다."));

        if (targetParticipant.getMafiaRole() == Mafia_Role.SOLDIER) {
            if (!targetParticipant.isSoldierShieldUsed()) {
                targetParticipant.useSoldierShield();
                return;
            }
        }

        targetParticipant.die();
        gameParticipantRepository.save(targetParticipant);
    }

    @Override
    public void processVote(MafiaVoteRequestDto request) {

    }

    @Override
    public void calculateDayResult(Long roomId) {

    }

    @Override
    public boolean checkGameEndCondition(Long roomId) {
        return false;
    }
}

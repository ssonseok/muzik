package son.suck.muzik.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import son.suck.muzik.domain.GameParticipant;
import son.suck.muzik.domain.GamePhase;
import son.suck.muzik.domain.GameRoom;
import son.suck.muzik.domain.Mafia_Role;
import son.suck.muzik.dto.MafiaExecutionVoteRequestDto;
import son.suck.muzik.dto.MafiaNightActionRequestDto;
import son.suck.muzik.dto.MafiaVoteRequestDto;
import son.suck.muzik.dto.PoliceInvestigationResultResponseDto;
import son.suck.muzik.repository.GameParticipantRepository;
import son.suck.muzik.repository.GameRoomRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MafiaPlayServiceImpl implements MafiaPlayService {

    private final GameRoomRepository gameRoomRepository;
    private final GameParticipantRepository gameParticipantRepository;

    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, MafiaNightActionRequestDto>> nightActionStore = new ConcurrentHashMap<>();

    // 1차 지목 투표: [방ID -> {투표자ID -> 지목된대상ID}]
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, Long>> nominationVotes = new ConcurrentHashMap<>();
    // 2차 찬반 투표: [방ID -> {투표자ID -> 찬성여부(true: 사형, false: 살림)}]
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, Boolean>> defenseVotes = new ConcurrentHashMap<>();
    // 현재 처형 후보자 저장용 (방ID -> 후보자참여자ID)
    private final ConcurrentHashMap<Long, Long> executionTargets = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void processNightAction(Long userId, MafiaNightActionRequestDto request) {
        System.out.println("🔥🔥 processNightAction 호출됨");
        System.out.println("userId = " + userId);
        System.out.println("roomId = " + request.getRoomId());
        System.out.println("targetId = " + request.getTargetId());
        System.out.println("actionType = " + request.getActionType());

        validateNightAction(userId, request);
        saveNightActionToStore(userId, request);
    }

    private void validateNightAction(Long userId, MafiaNightActionRequestDto request) {
        Long roomId = request.getRoomId();
        Long targetId = request.getTargetId();
        MafiaNightActionRequestDto.ActionType actionType = request.getActionType();

        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 게임방을 찾을 수 없습니다. Room ID: " + roomId));

        if (room.getGamePhase() != GamePhase.NIGHT) {
            throw new IllegalStateException("현재는 밤 페이즈가 아닙니다.");
        }

        GameParticipant participant = gameParticipantRepository
                .findByGameRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 방의 참여자 정보를 찾을 수 없습니다. User ID: " + userId));

        if (!participant.isAlive()) {
            throw new IllegalStateException(
                    "사망한 유저는 밤 행동을 수행할 수 없습니다.");
        }

        GameParticipant target = gameParticipantRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "행동 대상 참가자를 찾을 수 없습니다. Target ID: " + targetId));

        if (!target.getGameRoom().getId().equals(roomId)) {
            throw new IllegalArgumentException("해당 방의 참가자가 아닙니다.");
        }

        if (!target.isAlive()) {
            throw new IllegalStateException("사망한 참가자는 행동 대상으로 지정할 수 없습니다.");
        }

        Mafia_Role role = participant.getMafiaRole();

        boolean isMatched = switch (actionType) {
            case MAFIA_KILL -> role == Mafia_Role.MAFIA;
            case DOCTOR_HEAL -> role == Mafia_Role.DOCTOR;
            case POLICE_INVESTIGATE -> role == Mafia_Role.POLICE;
        };

        if (!isMatched) {
            throw new IllegalArgumentException(
                    "해당 직업은 이 밤 행동을 수행할 수 없습니다. 요청 액션: "
                            + actionType + ", 현재 직업: " + role);
        }
    }

    private void saveNightActionToStore(Long userId, MafiaNightActionRequestDto request) {
        Long roomId = request.getRoomId();
        System.out.println("🔥 밤 행동 저장 시도");
        System.out.println("🔥 userId = " + userId);
        System.out.println("🔥 roomId = " + roomId);
        System.out.println("🔥 actionType = " + request.getActionType());
        System.out.println("🔥 targetId = " + request.getTargetId());

        nightActionStore.putIfAbsent(roomId, new ConcurrentHashMap<>());
        ConcurrentHashMap<Long, MafiaNightActionRequestDto> roomActions = nightActionStore.get(roomId);

        roomActions.put(userId, request);
        System.out.println("🔥 저장 후 roomActions = " + roomActions);
    }

    @Override
    @Transactional
    public boolean calculateNightResult(Long roomId) {
        System.out.println("🔥 calculateNightResult 호출됨! roomId = " + roomId);

        ConcurrentHashMap<Long, MafiaNightActionRequestDto> roomActions =
                nightActionStore.get(roomId);
        System.out.println("🔥 roomActions = " + roomActions);
        System.out.println("🔥 roomActions size = " +
                (roomActions == null ? "null" : roomActions.size()));

        if (roomActions == null || roomActions.isEmpty()) {
            nightActionStore.remove(roomId);
            return false;
        }

        Long mafiaTargetId =
                determineMafiaTarget(roomActions);
        System.out.println("🔥 mafiaTargetId = " + mafiaTargetId);

        Long doctorTargetId =
                getDoctorTarget(roomActions);
        System.out.println("🔥 doctorTargetId = " + doctorTargetId);

        processPoliceInvestigation(roomActions);

        Long deadParticipantId =
                applyFinalSurvivalResult(
                        roomId,
                        mafiaTargetId,
                        doctorTargetId
                );
        System.out.println("🔥 deadParticipantId = " + deadParticipantId);

        // 밤 결과 전송
        if (deadParticipantId != null) {

            GameParticipant deadParticipant =
                    gameParticipantRepository.findById(deadParticipantId)
                            .orElse(null);

            if (deadParticipant != null) {

                messagingTemplate.convertAndSend(
                        "/sub/room/" + roomId + "/game",
                        Map.of(
                                "type", "NIGHT_RESULT",
                                "message",
                                deadParticipant.getUser().getNickname()
                                        + "이(가) 밤에 사망했습니다."
                        )
                );
            }

        } else {

            messagingTemplate.convertAndSend(
                    "/sub/room/" + roomId + "/game",
                    Map.of(
                            "type", "NIGHT_RESULT",
                            "message", "오늘 밤은 아무도 사망하지 않았습니다."
                    )
            );
        }

        boolean gameEnded =
                checkGameEndCondition(roomId);

        nightActionStore.remove(roomId);

        return gameEnded;
    }

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

    private Long getDoctorTarget(ConcurrentHashMap<Long, MafiaNightActionRequestDto> roomActions) {
        return roomActions.values().stream()
                .filter(action -> action.getActionType() == MafiaNightActionRequestDto.ActionType.DOCTOR_HEAL)
                .map(MafiaNightActionRequestDto::getTargetId)
                .findFirst()
                .orElse(null);
    }

    private void processPoliceInvestigation(ConcurrentHashMap<Long, MafiaNightActionRequestDto> roomActions) {
        roomActions.entrySet().stream()
                .filter(entry -> entry.getValue().getActionType() == MafiaNightActionRequestDto.ActionType.POLICE_INVESTIGATE)
                .forEach(entry -> {
                    Long policeUserId = entry.getKey();
                    MafiaNightActionRequestDto action = entry.getValue();
                    Long targetId = action.getTargetId();

                    GameParticipant targetParticipant = gameParticipantRepository.findById(targetId).orElse(null);

                    if (targetParticipant != null) {
                        boolean isMafia = (targetParticipant.getMafiaRole() == Mafia_Role.MAFIA);

                        String targetNickname = targetParticipant.getUser().getNickname();

                        PoliceInvestigationResultResponseDto responseDto =
                                new PoliceInvestigationResultResponseDto(targetId, targetNickname, isMafia);

                        String destination = "/sub/room/" + action.getRoomId() + "/police/" + policeUserId;

                        messagingTemplate.convertAndSend(destination, responseDto);

                        log.info("방 [{}] 경찰(유저ID: {}) 조사 완료 -> 대상: {} (마피아 여부: {})",
                                action.getRoomId(), policeUserId, targetNickname, isMafia);
                    }
                });
    }

    private Long applyFinalSurvivalResult(Long roomId, Long mafiaTargetId, Long doctorTargetId) {
        System.out.println(
                "🔥 applyFinalSurvivalResult 호출: mafiaTargetId="
                        + mafiaTargetId
                        + ", doctorTargetId="
                        + doctorTargetId
        );
        System.out.println("🔥 최종 밤 결과");
        System.out.println("mafiaTargetId = " + mafiaTargetId);
        System.out.println("doctorTargetId = " + doctorTargetId);
        if (mafiaTargetId == null) {
            return null;
        }

        if (mafiaTargetId.equals(doctorTargetId)) {
            return null;
        }

        GameParticipant targetParticipant =
                gameParticipantRepository.findById(mafiaTargetId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "마피아 타겟 참여자 정보를 찾을 수 없습니다."
                                ));

        if (!targetParticipant.getGameRoom().getId().equals(roomId)) {
            throw new IllegalArgumentException(
                    "해당 방의 참가자가 아닙니다."
            );
        }

        if (targetParticipant.getMafiaRole() == Mafia_Role.SOLDIER) {
            if (!targetParticipant.isSoldierShieldUsed()) {
                targetParticipant.useSoldierShield();
                return null;
            }
        }

        targetParticipant.die();
        System.out.println(
                "🔥 사망 처리: participantId = "
                        + targetParticipant.getId()
                        + ", alive = "
                        + targetParticipant.isAlive()
        );

        gameParticipantRepository.save(targetParticipant);

        return targetParticipant.getId();
    }

    // ==================================================================
    // 낮 투표 관련 구현 메서드
    // ===================================================================

    @Override
    public void processNominationVote(Long userId, MafiaVoteRequestDto request) {
        GameRoom room = findRoom(request.getRoomId());

        if (room.getGamePhase() != GamePhase.VOTE) {
            throw new IllegalStateException("지금은 1차 지목 투표 페이즈가 아닙니다.");
        }

        Long roomId = request.getRoomId();
        Long targetId = request.getTargetId();

        GameParticipant voter = gameParticipantRepository
                .findByGameRoomIdAndUserId(roomId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("투표자를 찾을 수 없습니다: " + userId));

        GameParticipant target = gameParticipantRepository.findById(targetId)
                .orElseThrow(() ->
                        new IllegalArgumentException("지목된 대상을 찾을 수 없습니다: " + targetId));

        if (!target.getGameRoom().getId().equals(roomId)) {
            throw new IllegalArgumentException("해당 방의 참가자가 아닙니다.");
        }

        if (!target.isAlive()) {
            throw new IllegalStateException("이미 사망한 참가자는 지목할 수 없습니다.");
        }

        if (!voter.isAlive()) {
            throw new IllegalStateException("사망자는 투표할 수 없습니다.");
        }

        nominationVotes
                .computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                .put(userId, targetId);
    }

    @Override
    public void processDefenseVote(Long userId, MafiaExecutionVoteRequestDto request) {
        GameRoom room = findRoom(request.getRoomId());
        if (room.getGamePhase() != GamePhase.DEFENSE) {
            throw new IllegalStateException("지금은 최후의 반론 찬반 투표 페이즈가 아닙니다.");
        }

        Long roomId = request.getRoomId();
        boolean isAgree = request.isAgree(); // true: 찬성(처형), false: 반대(생존)

        // voterId 대신 파라미터로 넘어온 userId 사용
        GameParticipant voter = gameParticipantRepository.findByGameRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("투표자를 찾을 수 없습니다: " + userId));

        if (!voter.isAlive()) {
            throw new IllegalStateException("사망자는 투표할 수 없습니다.");
        }

        defenseVotes
                .computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                .put(userId, isAgree);
    }

    @Override
    public boolean calculateDayResult(Long roomId) {
        GameRoom room = findRoom(roomId);

        if (room.getGamePhase() == GamePhase.VOTE) {
            processNominationResult(roomId);
            return false;
        } else if (room.getGamePhase() == GamePhase.DEFENSE) {
            return processDefenseResult(roomId);
        }

        return false;
    }

    @Override
    @Transactional
    public boolean checkGameEndCondition(Long roomId) {
        GameRoom room = findRoom(roomId);
        List<GameParticipant> participants = gameParticipantRepository.findByGameRoomId(roomId);

        // 살아있는 마피아 수 계산
        long aliveMafiaCount = participants.stream()
                .filter(GameParticipant::isAlive)
                .filter(p -> p.getMafiaRole() == Mafia_Role.MAFIA)
                .count();

        // 살아있는 총 생존자 수 계산
        long totalAliveCount = participants.stream()
                .filter(GameParticipant::isAlive)
                .count();

        long aliveCitizenCount = totalAliveCount - aliveMafiaCount;

        boolean isEnd = false;

        if (aliveMafiaCount == 0) {
            log.info("방 [{}] 게임 종료: 마피아가 모두 전멸하여 시민 팀이 승리했습니다!", roomId);
            isEnd = true;
        } else if (aliveMafiaCount >= aliveCitizenCount) {
            log.info("방 [{}] 게임 종료: 마피아 수({})가 시민 수({}) 이상이 되어 마피아 팀이 승리했습니다!", roomId, aliveMafiaCount, aliveCitizenCount);
            isEnd = true;
        }

        if (isEnd) {
            room.updateStatus("end");
            room.updatePhase(GamePhase.END);
            gameRoomRepository.save(room);

            messagingTemplate.convertAndSend(
                    "/sub/room/" + roomId + "/game-end",
                    Map.of("gamePhase", GamePhase.END)
            );

            return true;
        }

        return false;
    }

    @Override
    public boolean hasExecutionTarget(Long roomId) {
        return executionTargets.containsKey(roomId);
    }

    // ==================================================================
    // 내부 정산 헬퍼 메서드
    // ===================================================================

    private void processNominationResult(Long roomId) {
        ConcurrentHashMap<Long, Long> roomVotes = nominationVotes.remove(roomId);

        if (roomVotes == null || roomVotes.isEmpty()) {
            log.info("방 [{}] 접수된 투표 없음.", roomId);
            return;
        }

        Map<Long, Integer> voteCounts = new HashMap<>();
        for (Long targetId : roomVotes.values()) {
            voteCounts.put(targetId, voteCounts.getOrDefault(targetId, 0) + 1);
        }

        Long electedTargetId = null;
        int maxVotes = -1;
        boolean isTie = false;

        for (Map.Entry<Long, Integer> entry : voteCounts.entrySet()) {
            Long targetId = entry.getKey();
            int count = entry.getValue();

            if (count > maxVotes) {
                maxVotes = count;
                electedTargetId = targetId;
                isTie = false;
            } else if (count == maxVotes) {
                isTie = true;
            }
        }

        if (isTie || electedTargetId == null) {
            log.info("방 [{}] 최다 득표자 동률 발생 또는 표 없음.", roomId);

            messagingTemplate.convertAndSend(
                    "/sub/room/" + roomId + "/game",
                    Map.of(
                            "type", "VOTE_TIE",
                            "message", "투표 결과 동률입니다. 아무도 처형되지 않습니다."
                    )
            );

            return;
        }

        executionTargets.put(roomId, electedTargetId);
        log.info("방 [{}] 1차 투표 결과 최다 득표자 선정: ID [{}] ({}표)", roomId, electedTargetId, maxVotes);
    }

    private boolean processDefenseResult(Long roomId) {
        ConcurrentHashMap<Long, Boolean> roomDefenseVotes = defenseVotes.remove(roomId);
        Long targetId = executionTargets.remove(roomId);

        if (targetId == null) {
            log.info("방 [{}]에 처형 후보자가 없습니다.", roomId);
            return false;
        }

        int agreeCount = 0;
        int disagreeCount = 0;

        if (roomDefenseVotes != null) {
            for (boolean isAgree : roomDefenseVotes.values()) {
                if (isAgree) {
                    agreeCount++;
                } else {
                    disagreeCount++;
                }
            }
        }

        log.info("방 [{}] 찬반 투표 집계 - 찬성(처형): {}표, 반대(생존): {}표", roomId, agreeCount, disagreeCount);

        if (agreeCount > disagreeCount) {
            GameParticipant target = gameParticipantRepository.findById(targetId)
                    .orElseThrow(() -> new IllegalArgumentException("처형 대상 참여자를 찾을 수 없습니다: " + targetId));

            target.die();
            gameParticipantRepository.save(target);

            log.info("방 [{}] 투표 결과: 유죄 확정. 참여자 ID [{}] 처형 집행 완료.", roomId, targetId);
        } else {
            log.info("방 [{}] 투표 결과: 무죄 방면. 과반수 찬성을 얻지 못해 아무도 처형되지 않습니다.", roomId);
        }

        return checkGameEndCondition(roomId);
    }

    private GameRoom findRoom(Long roomId) {
        return gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다: " + roomId));
    }
}
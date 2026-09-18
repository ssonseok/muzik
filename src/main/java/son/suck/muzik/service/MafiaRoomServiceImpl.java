package son.suck.muzik.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import son.suck.muzik.domain.*;
import son.suck.muzik.dto.*;
import son.suck.muzik.repository.GameParticipantRepository;
import son.suck.muzik.repository.GameRoomRepository;
import son.suck.muzik.repository.UsersRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MafiaRoomServiceImpl implements MafiaRoomService {

    private final GameRoomRepository gameRoomRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final UsersRepository usersRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public MafiaRoomResponse createRoom(MafiaCreateRoomRequestDto request, Long hostUserId) {

        Users hostUser = usersRepository.findById(hostUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        GameRoom gameRoom = GameRoom.miniGameRoomBuilder()
                .roomName(request.getRoomName())
                .maxPlayers(request.getMaxPlayers())
                .password(null)
                .roomStatus("WAITING")
                .roomType(RoomType.mafia)
                .build();

        GameRoom savedRoom = gameRoomRepository.save(gameRoom);

        GameParticipant hostParticipant = GameParticipant.builder()
                .user(hostUser)
                .gameRoom(savedRoom)
                .isHost(true)
                .build();

        gameParticipantRepository.save(hostParticipant);

        return new MafiaRoomResponse(savedRoom);
    }

    @Override
    public List<MafiaRoomResponse> getRoomList() {
        List<GameRoom> rooms = gameRoomRepository.findByRoomTypeAndRoomStatusOrderByIdDesc(RoomType.mafia, "WAITING");

        return rooms.stream()
                .map(MafiaRoomResponse::new)
                .toList();
    }

    @Override
    @Transactional
    public void joinRoom(Long roomId, Long userId) {
        GameRoom gameRoom = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (!"WAITING".equals(gameRoom.getRoomStatus())) {
            throw new IllegalStateException("이미 게임이 시작되었거나 종료된 방입니다.");
        }

        if (gameRoom.getParticipants().size() >= gameRoom.getMaxPlayers()) {
            throw new IllegalArgumentException("방이 가득 찼습니다.");
        }

        boolean alreadyJoined = gameRoom.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));
        if (alreadyJoined) {
            throw new IllegalStateException("이미 입장해 있는 방입니다.");
        }

        GameParticipant participant = GameParticipant.builder()
                .user(user)
                .gameRoom(gameRoom)
                .isHost(false)
                .build();

        gameParticipantRepository.save(participant);

        messagingTemplate.convertAndSend(
                "/sub/room/" + roomId + "/participants",
                "UPDATE"
        );
    }

    @Override
    @Transactional
    public void leaveRoom(Long roomId, Long userId) {
        GameRoom gameRoom = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        GameParticipant targetParticipant = gameRoom.getParticipants().stream()
                .filter(p -> p.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("해당 방에 참여하고 있지 않습니다."));

        boolean wasHost = targetParticipant.isHost();

        gameParticipantRepository.delete(targetParticipant);
        gameRoom.getParticipants().remove(targetParticipant); // 컬렉션에서도 동기화

        // 방에 사람없으면 삭제
        if (gameRoom.getParticipants().isEmpty()) {
            gameRoomRepository.delete(gameRoom);
            return;
        }

        //방장퇴장하면 남은사람한테 위임
        if (wasHost && !gameRoom.getParticipants().isEmpty()) {
            GameParticipant newHost = gameRoom.getParticipants().get(0); // 첫 번째 남은 사람
            newHost.updateHost(true);
        }

        messagingTemplate.convertAndSend(
                "/sub/room/" + roomId + "/participants",
                "UPDATE"
        );
    }

    @Override
    @Transactional
    public void startGame(Long roomId, Long hostUserId) {
        GameRoom gameRoom = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        boolean isHost = gameRoom.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(hostUserId) && p.isHost());

        if (!isHost) {
            throw new IllegalStateException("방장만 게임을 시작할 수 있습니다.");
        }

        if (!"WAITING".equals(gameRoom.getRoomStatus())) {
            throw new IllegalStateException("이미 게임이 시작되었거나 대기 중이 아닌 방입니다.");
        }

        int totalPlayers = gameRoom.getParticipants().size();

        if (totalPlayers < 4) {
            throw new IllegalStateException("게임을 시작하려면 최소 4명의 인원이 필요합니다.");
        }

        if (totalPlayers > 12) {
            throw new IllegalStateException("게임 최대 인원은 12명입니다.");
        }

        gameRoom.updateStatus("PLAYING");

        assignRolesToParticipants(
                gameRoom.getParticipants(),
                totalPlayers
        );

        eventPublisher.publishEvent(
                new MafiaStartEventDto(roomId, totalPlayers)
        );
    }

    //역할 배분 헬퍼 메서드
    private void assignRolesToParticipants(List<GameParticipant> participants, int totalPlayers) {
        List<Mafia_Role> roles = new ArrayList<>();

        //마피아 수 산정 (4~6명: 1명 / 7~10명: 2명 / 11~12명: 3명)
        int mafiaCount = 1;
        if (totalPlayers >= 7 && totalPlayers <= 10) {
            mafiaCount = 2;
        } else if (totalPlayers >= 11) {
            mafiaCount = 3;
        }

        for (int i = 0; i < mafiaCount; i++) {
            roles.add(Mafia_Role.MAFIA);
        }

        roles.add(Mafia_Role.POLICE);

        if (totalPlayers >= 5) {
            roles.add(Mafia_Role.DOCTOR);
        }

        if (totalPlayers >= 6) {
            roles.add(Mafia_Role.SOLDIER);
        }

        while (roles.size() < totalPlayers) {
            roles.add(Mafia_Role.CITIZEN);
        }

        Collections.shuffle(roles);

        for (int i = 0; i < totalPlayers; i++) {
            participants.get(i).assignRole(roles.get(i));
        }
    }

    @Override
    @Transactional
    public void updateRoomPhase(Long roomId, GamePhase phase) {
        GameRoom gameRoom = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));
        gameRoom.updatePhase(phase);
    }

    @Override
    public MafiaMyInfoResponse getMyInfo(Long roomId, Long userId) {
        GameParticipant participant = gameParticipantRepository.findByGameRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("참여 정보를 찾을 수 없습니다. roomId: " + roomId + ", userId: " + userId));

        return new MafiaMyInfoResponse(
                participant.getId(),
                participant.getUser().getNickname(),
                participant.getMafiaRole(),
                participant.isAlive(),
                participant.isHost(),
                participant.getGameRoom().getGamePhase()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MafiaParticipantResponse> getParticipants(Long roomId) {

        List<GameParticipant> participants =
                gameParticipantRepository.findByGameRoomId(roomId);

        return participants.stream()
                .map(participant -> new MafiaParticipantResponse(
                        participant.getId(),
                        participant.getUser().getNickname(),
                        participant.isAlive(),
                        participant.isHost()
                ))
                .toList();
    }
}

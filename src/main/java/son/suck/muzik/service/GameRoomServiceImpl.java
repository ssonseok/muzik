package son.suck.muzik.service;

import son.suck.muzik.domain.GameParticipant;
import son.suck.muzik.domain.GameRoom;
import son.suck.muzik.domain.UserStatus;
import son.suck.muzik.domain.Users;
import son.suck.muzik.dto.CreateRoomRequest;
import son.suck.muzik.dto.GameRoomDetailResponse;
import son.suck.muzik.dto.GameRoomResponse;
import son.suck.muzik.repository.GameParticipantRepository;
import son.suck.muzik.repository.GameRoomRepository;
import son.suck.muzik.repository.UserStatusRepository;
import son.suck.muzik.repository.UsersRepository;
import son.suck.muzik.service.GameRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameRoomServiceImpl implements GameRoomService {

    private final GameRoomRepository gameRoomRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final UsersRepository usersRepository;
    private final UserStatusRepository userStatusRepository;

    /**
     * 1. 방 생성 + 방장 자동 입장
     */
    @Override
    @Transactional
    public GameRoomResponse createRoom(CreateRoomRequest request) {
        Users hostUser = usersRepository.findById(request.getHostUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. ID: " + request.getHostUserId()));

        GameRoom gameRoom = GameRoom.builder()
                .roomName(request.getRoomName())
                .maxPlayers(request.getMaxPlayers())
                .roomStatus("WAITING")
                .password(request.getPassword())
                .genre(request.getGenre())
                .startYear(request.getStartYear())
                .endYear(request.getEndYear())
                .musicCount(request.getMusicCount())
                .build();

        GameRoom savedRoom = gameRoomRepository.save(gameRoom);

        // 방 참여자 정보에 방장 등록
        GameParticipant host = GameParticipant.builder()
                .gameRoom(savedRoom)
                .user(hostUser)
                .isHost(true)
                .build();

        gameParticipantRepository.save(host);

        return new GameRoomResponse(savedRoom);
    }

    /**
     * 2. 대기실 "WAITING" 방 목록 조회
     */
    @Override
    public List<GameRoomResponse> getWaitingRooms() {
        return gameRoomRepository.findByRoomStatusOrderByIdDesc("WAITING").stream()
                .map(GameRoomResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * 3. 일반 유저 방 입장
     */
    @Override
    @Transactional
    public void enterRoom(Long roomId, Long userId) {
        GameRoom gameRoom = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        if (!"WAITING".equals(gameRoom.getRoomStatus())) {
            throw new IllegalStateException("이미 게임이 시작되었거나 입장할 수 없는 방입니다.");
        }

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. ID: " + userId));

        boolean isAlreadyJoined = gameRoom.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));

        if (isAlreadyJoined) {
            return; // 이미 들어가 있으면 추가 저장 없이 정상 반환
        }

        // 정원 초과 검사
        if (gameRoom.getParticipants().size() >= gameRoom.getMaxPlayers()) {
            throw new IllegalStateException("방이 꽉 차서 입장할 수 없습니다.");
        }

        boolean isHost = gameRoom.getParticipants().isEmpty();

        GameParticipant participant = GameParticipant.builder()
                .gameRoom(gameRoom)
                .user(user)
                .isHost(isHost)
                .build();

        gameParticipantRepository.save(participant);
        gameRoom.getParticipants().add(participant);

        UserStatus userStatus = userStatusRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("UserStatus를 찾을 수 없습니다."));
        userStatus.updateInRoom(roomId);
    }

    /**
     * 4. 게임방 상세 정보 + 참여자 목록 조회
     */
    @Override
    public GameRoomDetailResponse getRoomDetail(Long roomId) {
        GameRoom gameRoom = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다. ID: " + roomId));

        return new GameRoomDetailResponse(gameRoom);
    }

    /**
     * 5. 게임방 퇴장 (방장 위임 및 빈 방 삭제 포함)
     */
    @Override
    @Transactional
    public void leaveRoom(Long roomId, Long userId) {
        GameRoom gameRoom = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다. ID: " + roomId));

        // 해당 유저가 방 참여자인지 확인
        GameParticipant participant = gameParticipantRepository.findByGameRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 방의 참여자가 아닙니다. User ID: " + userId));

        boolean wasHost = participant.isHost();

        //참여자 목록에서 삭제
        gameRoom.getParticipants().remove(participant);
        gameParticipantRepository.delete(participant);
        gameParticipantRepository.flush();

        userStatusRepository.findByUserId(userId)
                .ifPresent(UserStatus::updateOnline);

        // 남아있는 참여자가 없는 경우 -> 방 삭제
        if (gameRoom.getParticipants().isEmpty()) {
            gameRoomRepository.delete(gameRoom);
            return;
        }

        // 방장이 나갔고 남아있는 유저가 있는 경우 -> 방장 권한 위임
        if (wasHost) {
            GameParticipant nextHost = gameRoom.getParticipants().get(0); // 가장 먼저 들어온 유저
            nextHost.updateHost(true); // isHost = true 변경 (엔티티 메서드 필요)
        }

    }
}
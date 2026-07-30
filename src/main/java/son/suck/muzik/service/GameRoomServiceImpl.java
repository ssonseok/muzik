package son.suck.muzik.service;

import son.suck.muzik.domain.GameParticipant;
import son.suck.muzik.domain.GameRoom;
import son.suck.muzik.domain.Users;
import son.suck.muzik.dto.CreateRoomRequest;
import son.suck.muzik.dto.GameRoomResponse;
import son.suck.muzik.repository.GameParticipantRepository;
import son.suck.muzik.repository.GameRoomRepository;
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

        GameParticipant participant = GameParticipant.builder()
                .gameRoom(gameRoom)
                .user(user)
                .isHost(false)
                .build();

        gameParticipantRepository.save(participant);
    }
}
package son.suck.muzik.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import son.suck.muzik.domain.GameParticipant;
import son.suck.muzik.domain.GameRoom;
import son.suck.muzik.domain.RoomType;
import son.suck.muzik.domain.Users;
import son.suck.muzik.dto.MafiaCreateRoomRequestDto;
import son.suck.muzik.dto.MafiaRoomResponse;
import son.suck.muzik.repository.GameParticipantRepository;
import son.suck.muzik.repository.GameRoomRepository;
import son.suck.muzik.repository.UsersRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MafiaRoomServiceImpl implements MafiaRoomService{

    private final GameRoomRepository gameRoomRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final UsersRepository usersRepository;

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
    }

    @Override
    public void startGame(Long roomId, Long hostUserId) {

    }
}

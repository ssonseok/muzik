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
        return List.of();
    }

    @Override
    public void joinRoom(Long roomId, Long userId) {

    }

    @Override
    public void leaveRoom(Long roomId, Long userId) {

    }

    @Override
    public void startGame(Long roomId, Long hostUserId) {

    }
}

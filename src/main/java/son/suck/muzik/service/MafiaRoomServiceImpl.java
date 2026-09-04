package son.suck.muzik.service;

import son.suck.muzik.dto.MafiaCreateRoomRequestDto;
import son.suck.muzik.dto.MafiaRoomResponse;

import java.util.List;

public class MafiaRoomServiceImpl implements MafiaRoomService{

    @Override
    public MafiaRoomResponse createRoom(MafiaCreateRoomRequestDto request, Long hostUserId) {
        return null;
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

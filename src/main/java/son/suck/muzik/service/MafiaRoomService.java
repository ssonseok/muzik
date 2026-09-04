package son.suck.muzik.service;

import son.suck.muzik.dto.MafiaCreateRoomRequestDto;
import son.suck.muzik.dto.MafiaRoomResponse;

import java.util.List;

public interface MafiaRoomService {
    MafiaRoomResponse createRoom(MafiaCreateRoomRequestDto request, Long hostUserId);
    List<MafiaRoomResponse> getRoomList();
    void joinRoom(Long roomId, Long userId);
    void leaveRoom(Long roomId, Long userId);
    void startGame(Long roomId, Long hostUserId);
}
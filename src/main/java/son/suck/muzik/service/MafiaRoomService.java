package son.suck.muzik.service;

import son.suck.muzik.dto.MafiaCreateRoomRequestDto;
import son.suck.muzik.dto.MafiaRoomResponse;

public interface MafiaRoomService {
    MafiaRoomResponse createRoom(MafiaCreateRoomRequestDto request);
    void joinRoom(Long roomId, Long userId, String password);
    void leaveRoom(Long roomId, Long userId);
    //시작할때 직업 분배랑 밤으로 가기
    void startGame(Long roomId, Long hostUserId);
}

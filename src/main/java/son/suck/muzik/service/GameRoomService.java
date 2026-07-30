package son.suck.muzik.service;

import son.suck.muzik.dto.CreateRoomRequest;
import son.suck.muzik.dto.GameRoomResponse;

import java.util.List;

public interface GameRoomService {
    // 방 만들기
    GameRoomResponse createRoom(CreateRoomRequest request);
    // 대기실 waiting 방 목록 조회
    List<GameRoomResponse> getWaitingRooms();
    // 방입장
    void enterRoom(Long roomId, Long userId);
}

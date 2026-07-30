package son.suck.muzik.service;

import son.suck.muzik.dto.CreateRoomRequest;
import son.suck.muzik.dto.GameRoomDetailResponse;
import son.suck.muzik.dto.GameRoomResponse;

import java.util.List;

public interface GameRoomService {
    // 방 만들기
    GameRoomResponse createRoom(CreateRoomRequest request);
    // 대기실 waiting 방 목록 조회
    List<GameRoomResponse> getWaitingRooms();
    // 방입장
    void enterRoom(Long roomId, Long userId);
    // 게임방 참여자 목록 조회
    GameRoomDetailResponse getRoomDetail(Long roomId);
    // 게임방 퇴장
    void leaveRoom(Long roomId, Long userId);
}

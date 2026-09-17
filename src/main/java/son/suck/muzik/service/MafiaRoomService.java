package son.suck.muzik.service;

import son.suck.muzik.domain.GamePhase;
import son.suck.muzik.dto.MafiaCreateRoomRequestDto;
import son.suck.muzik.dto.MafiaMyInfoResponse;
import son.suck.muzik.dto.MafiaParticipantResponse;
import son.suck.muzik.dto.MafiaRoomResponse;

import java.util.List;

public interface MafiaRoomService {
    MafiaRoomResponse createRoom(MafiaCreateRoomRequestDto request, Long hostUserId);
    List<MafiaRoomResponse> getRoomList();
    void joinRoom(Long roomId, Long userId);
    void leaveRoom(Long roomId, Long userId);
    void startGame(Long roomId, Long hostUserId);
    void updateRoomPhase(Long roomId, GamePhase phase);
    //직업,생존상태조회
    MafiaMyInfoResponse getMyInfo(Long roomId, Long userId);
    List<MafiaParticipantResponse> getParticipants(Long roomId);
}
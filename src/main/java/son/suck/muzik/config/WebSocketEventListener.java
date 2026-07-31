package son.suck.muzik.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import son.suck.muzik.dto.GameRoomResponse;
import son.suck.muzik.service.GameRoomService;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final GameRoomService gameRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 웹소켓 연결이 끊어졌을 때 (브라우저 닫음, 네트워크 끊김 등) 자동으로 실행
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Long roomId = (Long) headerAccessor.getSessionAttributes().get("roomId");
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");

        if (roomId != null && userId != null) {
            log.info("비정상 연결 종료 감지 - Room ID: {}, User ID: {}", roomId, userId);

            try {
                // 방 퇴장 로직 (방장 위임 / 빈 방 삭제 )
                gameRoomService.leaveRoom(roomId, userId);

                // 퇴장 반영된 최신 방 목록을 로비로 실시간 방송
                List<GameRoomResponse> updatedRooms = gameRoomService.getWaitingRooms();
                messagingTemplate.convertAndSend("/topic/lobby", updatedRooms);

            } catch (Exception e) {
                log.error("자동 퇴장 처리 중 오류 발생: {}", e.getMessage());
            }
        }
    }
}

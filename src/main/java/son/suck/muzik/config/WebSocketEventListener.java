package son.suck.muzik.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import son.suck.muzik.domain.UserStatus;
import son.suck.muzik.dto.ChatMessageDto;
import son.suck.muzik.dto.GameRoomDetailResponse;
import son.suck.muzik.dto.GameRoomResponse;
import son.suck.muzik.repository.UserStatusRepository;
import son.suck.muzik.service.GameRoomService;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final GameRoomService gameRoomService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserStatusRepository userStatusRepository;

    @EventListener
    @Transactional
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        if (headerAccessor.getSessionAttributes() != null) {
            Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");

            if (userId != null) {
                userStatusRepository.findByUserId(userId)
                        .ifPresent(UserStatus::updateOnline);
                log.info("웹소켓 연결 감지 - 유저 ID: {} [ONLINE]", userId);
            }
        }
    }

    @EventListener
    @Transactional
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        if (headerAccessor.getSessionAttributes() == null) return;

        Long roomId = (Long) headerAccessor.getSessionAttributes().get("roomId");
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        String nickname = (String) headerAccessor.getSessionAttributes().get("nickname");
        String leaverName = (nickname != null) ? nickname : "참여자";

        if (roomId != null && userId != null) {
            log.info("비정상 연결 종료 감지 - Room ID: {}, User ID: {}", roomId, userId);

            try {
                // 방 퇴장 로직 (DB 삭제, 방장 위임, 빈 방 삭제)
                gameRoomService.leaveRoom(roomId, userId);

                // 방이 아직 남아있는지 확인 후 내부 방송
                try {
                    GameRoomDetailResponse roomDetail = gameRoomService.getRoomDetail(roomId);

                    // 방 채팅창에 퇴장 알림
                    ChatMessageDto leaveNotice = new ChatMessageDto();
                    leaveNotice.setRoomId(roomId);
                    leaveNotice.setType("LEAVE");
                    leaveNotice.setMessage(leaverName + "님이 방을 나갔습니다.");
                    messagingTemplate.convertAndSend("/topic/rooms/" + roomId, leaveNotice);

                    //방 유저 목록  & 방장  표시 실시간 갱신용 방송
                    messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/info", roomDetail);

                } catch (IllegalArgumentException e) {
                    // 마지막 사람이 나가서 방이 DB에서 삭제되었을 때 예외 처리 (정상 상황)
                    log.info("모든 유저가 나가서 방이 정리되었습니다. Room ID: {}", roomId);
                }

                // 게임방 상황들
                List<GameRoomResponse> updatedRooms = gameRoomService.getWaitingRooms();
                messagingTemplate.convertAndSend("/topic/lobby", updatedRooms);

            } catch (Exception e) {
                log.error("자동 퇴장 처리 중 오류 발생: {}", e.getMessage());
            }
        }

        if (userId != null) {
            userStatusRepository.findByUserId(userId)
                    .ifPresent(UserStatus::updateOffline);
            log.info("웹소켓 연결 종료 - 유저 ID: {} [OFFLINE]", userId);
        }
    }
}

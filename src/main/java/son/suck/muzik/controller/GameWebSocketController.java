package son.suck.muzik.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import son.suck.muzik.dto.ChatMessageDto;
import son.suck.muzik.service.GamePlayService;
import son.suck.muzik.service.GameRoomService;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final GamePlayService gamePlayService;
    private final GameRoomService gameRoomService;

    /**
     * 유저가 게임방 안에서 채팅/명령을 치면 수신하는 웹소켓 단일 창구
     * 프론트엔드가 /app/game/chat 으로 메시지를 보내면 실행됨
     */
    @MessageMapping("/game/chat")
    public void broadcastMessage(@Payload ChatMessageDto message,
                                 SimpMessageHeaderAccessor headerAccessor) {

        ChatMessageDto processedMessage;

        // 방 입장 (ENTER)
        if ("ENTER".equals(message.getType())) {
            // 웹소켓 세션 속성에 roomId, userId, nickname 바인딩 (퇴장 이벤트 등 처리를 위해 저장)
            if (headerAccessor.getSessionAttributes() != null) {
                headerAccessor.getSessionAttributes().put("roomId", message.getRoomId());
                headerAccessor.getSessionAttributes().put("userId", message.getSenderId());
                headerAccessor.getSessionAttributes().put("nickname", message.getSender());
            }
            //  서비스의 processEnter를 호출하여 DB 검증 및 입장 메시지 처리
            processedMessage = gamePlayService.processEnter(message);

            messagingTemplate.convertAndSend("/topic/lobby", gameRoomService.getWaitingRooms());
        }
        // 게임 시작
        else if ("START".equals(message.getType())) {
            processedMessage = gamePlayService.processStart(message);
        }
        // 라운드 스킵 투표
        else if ("SKIP".equals(message.getType())) {
            processedMessage = gamePlayService.processSkip(message);
        }
        // 일반 채팅 및 정답 검증
        else {
            processedMessage = gamePlayService.checkAnswer(message);
        }

        // 구독 주소 예시: /topic/rooms/1
        messagingTemplate.convertAndSend("/topic/rooms/" + message.getRoomId(), processedMessage);
    }
    /**
     * 유저가 브라우저 탭을 닫거나 네트워크가 끊겨 웹소켓 세션이 종료될 때 자동 실행
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        if (headerAccessor.getSessionAttributes() != null) {
            Long roomId = (Long) headerAccessor.getSessionAttributes().get("roomId");
            Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
            String nickname = (String) headerAccessor.getSessionAttributes().get("nickname");

            if (roomId != null && userId != null) {
                log.info("[WebSocket] 유저 세션 끊김 감지 - RoomId: {}, Nickname: {}", roomId, nickname);

                // 1. 기존 leaveRoom API 서비스 로직 재활용 (DB에서 참가자 삭제 및 방장 위임 등)
                gameRoomService.leaveRoom(roomId, userId);

                // 2. 퇴장 메세지를 해당 방 유저들에게 브로드캐스트
                ChatMessageDto leaveNotice = new ChatMessageDto();
                leaveNotice.setRoomId(roomId);
                leaveNotice.setSender(nickname);
                leaveNotice.setType("LEAVE");
                leaveNotice.setMessage(nickname + "님의 연결이 끊어져 퇴장하셨습니다.");

                messagingTemplate.convertAndSend("/topic/rooms/" + roomId, leaveNotice);

                // 3. 로비 유저들에게도 변경된 방 목록 방송
                messagingTemplate.convertAndSend("/topic/lobby", gameRoomService.getWaitingRooms());
            }
        }
    }
}
package son.suck.muzik.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import son.suck.muzik.dto.ChatMessageDto;

@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * 유저가 게임방 안에서 채팅을 치면 수신하는 주소
     * 프론트엔드가 /app/game/chat 으로 메시지를 보내면 이 메서드가 실행
     */
    @MessageMapping("/game/chat")
    public void broadcastMessage(@Payload ChatMessageDto message) {

        // 만약 유저가 처음 방에 들어온 'ENTER' 타입이라면 안내 문구 셋팅
        if ("ENTER".equals(message.getType())) {
            message.setMessage(message.getSender() + "님이 입장하셨습니다.");
        }

        // 해당 방을 보기로 구독한 사람들에게만 채팅 메시지를 퍼트림
        // 방송 주소 예시: /topic/rooms/1, /topic/rooms/5
        messagingTemplate.convertAndSend("/topic/rooms/" + message.getRoomId(), message);
    }
}
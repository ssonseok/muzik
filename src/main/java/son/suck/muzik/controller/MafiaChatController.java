package son.suck.muzik.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import son.suck.muzik.dto.MafiaChatMessageDto;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MafiaChatController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 낮 전체 채팅 (누구나 참여 가능)
     * 프론트 구독 경로: /sub/room/{roomId}/chat
     * 프론트 전송 경로: /pub/room/{roomId}/chat
     */
    @MessageMapping("/room/{roomId}/chat")
    public void sendGeneralChat(@DestinationVariable Long roomId, MafiaChatMessageDto message) {
        messagingTemplate.convertAndSend("/sub/room/" + roomId + "/chat", message);
    }

    /**
     * 마피아 전용 밤 비밀 채팅 (마피아만 참여 가능)
     * 프론트 구독 경로: /sub/room/{roomId}/mafia-chat
     * 프론트 전송 경로: /pub/room/{roomId}/mafia-chat
     */
    @MessageMapping("/room/{roomId}/mafia-chat")
    public void sendMafiaChat(@DestinationVariable Long roomId, MafiaChatMessageDto message) {
        // TODO: 요청 보낸 유저가 실제로 마피아인지 검증하는 로직
        messagingTemplate.convertAndSend("/sub/room/" + roomId + "/mafia-chat", message);
    }
}

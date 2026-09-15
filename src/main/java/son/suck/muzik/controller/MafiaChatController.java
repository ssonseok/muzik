package son.suck.muzik.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import son.suck.muzik.dto.MafiaChatMessageDto;
import son.suck.muzik.service.MafiaChatService;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MafiaChatController {

    private final MafiaChatService mafiaChatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 낮 전체 채팅 (누구나 참여 가능)
     * 프론트 구독 경로: /sub/room/{roomId}/chat
     * 프론트 전송 경로: /pub/room/{roomId}/chat
     */
    @MessageMapping("/room/{roomId}/chat")
    public void sendGeneralChat(
            @DestinationVariable Long roomId,
            MafiaChatMessageDto message) {

        messagingTemplate.convertAndSend(
                "/sub/room/" + roomId + "/chat",
                message
        );
    }

    /**
     * 마피아 전용 밤 비밀 채팅 (마피아만 참여 가능)
     * 프론트 구독 경로: /sub/room/{roomId}/mafia-chat
     * 프론트 전송 경로: /pub/room/{roomId}/mafia-chat
     */
    @MessageMapping("/room/{roomId}/mafia-chat")
    public void sendMafiaChat(
            @DestinationVariable Long roomId,
            MafiaChatMessageDto message,
            SimpMessageHeaderAccessor accessor) {

        Map<String, Object> sessionAttributes =
                accessor.getSessionAttributes();

        if (sessionAttributes == null ||
                sessionAttributes.get("userId") == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        Long userId = (Long) sessionAttributes.get("userId");

        mafiaChatService.sendMafiaChat(
                roomId,
                userId,
                message
        );
    }
}
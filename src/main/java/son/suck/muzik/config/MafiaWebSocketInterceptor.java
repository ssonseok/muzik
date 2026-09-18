package son.suck.muzik.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import son.suck.muzik.domain.GameParticipant;
import son.suck.muzik.domain.Mafia_Role;
import son.suck.muzik.repository.GameParticipantRepository;

import java.util.Map;

@Component
public class MafiaWebSocketInterceptor
        implements ChannelInterceptor {

    private final GameParticipantRepository gameParticipantRepository;

    public MafiaWebSocketInterceptor(
            GameParticipantRepository gameParticipantRepository) {

        this.gameParticipantRepository =
                gameParticipantRepository;
    }

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        // SUBSCRIBE만 검사
        if (accessor != null &&
                StompCommand.SUBSCRIBE.equals(
                        accessor.getCommand())) {

            String destination =
                    accessor.getDestination();

            if (destination == null) {
                return message;
            }

            // 마피아 채팅 구독인지 확인
            if (destination.matches(
                    "^/sub/room/\\d+/mafia-chat$")) {

                // 로그인 확인
                Map<String, Object> sessionAttributes =
                        accessor.getSessionAttributes();

                if (sessionAttributes == null ||
                        sessionAttributes.get("userId") == null) {

                    throw new IllegalStateException(
                            "로그인이 필요합니다."
                    );
                }

                Long userId =
                        (Long) sessionAttributes.get("userId");

                // roomId 추출
                String[] parts =
                        destination.split("/");

                Long roomId =
                        Long.valueOf(parts[3]);

                // 해당 방 참가자인지 확인
                GameParticipant participant =
                        gameParticipantRepository
                                .findByGameRoomIdAndUserId(
                                        roomId,
                                        userId
                                )
                                .orElseThrow(() ->
                                        new IllegalStateException(
                                                "해당 게임방의 참가자가 아닙니다."
                                        )
                                );

                // 마피아인지 확인
                if (participant.getMafiaRole()
                        != Mafia_Role.MAFIA) {

                    throw new IllegalStateException(
                            "마피아만 사용할 수 있는 채팅입니다."
                    );
                }
            }
        }

        return message;
    }
}


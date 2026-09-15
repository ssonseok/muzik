package son.suck.muzik.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import son.suck.muzik.domain.GameParticipant;
import son.suck.muzik.domain.Mafia_Role;
import son.suck.muzik.dto.MafiaChatMessageDto;
import son.suck.muzik.repository.GameParticipantRepository;

@Service
@RequiredArgsConstructor
public class MafiaChatServiceImpl implements MafiaChatService {

    private final GameParticipantRepository gameParticipantRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendMafiaChat(
            Long roomId,
            Long userId,
            MafiaChatMessageDto message) {

        GameParticipant participant =
                gameParticipantRepository
                        .findByGameRoomIdAndUserId(roomId, userId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("게임방 참가자가 아닙니다."));

        // 죽은 유저는 마피아 채팅 사용 불가
        if (!participant.isAlive()) {
            throw new IllegalStateException(
                    "죽은 유저는 마피아 채팅을 사용할 수 없습니다.");
        }

        // 마피아만 사용 가능
        if (participant.getMafiaRole() != Mafia_Role.MAFIA) {
            throw new IllegalStateException(
                    "마피아만 사용할 수 있는 채팅입니다.");
        }

        messagingTemplate.convertAndSend(
                "/sub/room/" + roomId + "/mafia-chat",
                message
        );
    }
}

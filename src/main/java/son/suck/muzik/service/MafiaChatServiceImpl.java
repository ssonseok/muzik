package son.suck.muzik.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import son.suck.muzik.domain.GameParticipant;
import son.suck.muzik.domain.GamePhase;
import son.suck.muzik.domain.GameRoom;
import son.suck.muzik.domain.Mafia_Role;
import son.suck.muzik.dto.MafiaChatMessageDto;
import son.suck.muzik.repository.GameParticipantRepository;

@Service
@RequiredArgsConstructor
public class MafiaChatServiceImpl implements MafiaChatService {

    private final GameParticipantRepository gameParticipantRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MafiaPlayService mafiaPlayService;


    @Override
    @Transactional
    public void sendGeneralChat(
            Long roomId,
            Long userId,
            MafiaChatMessageDto message) {

        GameParticipant participant =
                gameParticipantRepository
                        .findByGameRoomIdAndUserId(roomId, userId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "게임방 참가자가 아닙니다."
                                )
                        );

        // 사망자는 어떤 페이즈에서도 채팅 불가
        if (!participant.isAlive()) {
            throw new IllegalStateException(
                    "죽은 유저는 채팅을 사용할 수 없습니다."
            );
        }

        GameRoom room = participant.getGameRoom();

        // 밤에는 전체 채팅 금지
        if (room.getGamePhase() == GamePhase.NIGHT) {
            throw new IllegalStateException(
                    "밤에는 전체 채팅을 사용할 수 없습니다."
            );
        }

        // 최후 반론에서는 처형 후보만 채팅 가능
        if (room.getGamePhase() == GamePhase.DEFENSE) {

            if (!mafiaPlayService.isExecutionTarget(
                    roomId,
                    participant.getId()
            )) {
                throw new IllegalStateException(
                        "최후 반론 대상만 채팅할 수 있습니다."
                );
            }
        }

        message.setRoomId(roomId);
        message.setSenderId(userId);
        message.setSenderName(
                participant.getUser().getNickname()
        );

        messagingTemplate.convertAndSend(
                "/sub/room/" + roomId + "/chat",
                message
        );
    }

    @Override
    @Transactional
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
        message.setRoomId(roomId);
        message.setSenderId(userId);
        message.setSenderName( participant.getUser().getNickname() );

        messagingTemplate.convertAndSend(
                "/sub/room/" + roomId + "/mafia-chat",
                message
        );
    }
}

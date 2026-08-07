package son.suck.muzik.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessageDto {
    private Long roomId;
    private Long senderId;
    private String sender;
    private String message;
    private String type;  // 메시지 타입 (TALK: 채팅, ENTER: 입장, ANSWER: 정답 시도 등)
    private String youtubeId;
    private Integer round;
}

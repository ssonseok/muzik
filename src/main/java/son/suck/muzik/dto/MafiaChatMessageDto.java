package son.suck.muzik.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MafiaChatMessageDto {
    private Long roomId;
    private Long senderId;
    private String senderName;
    private String message;
}

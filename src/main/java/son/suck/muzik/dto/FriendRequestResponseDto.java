package son.suck.muzik.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FriendRequestResponseDto {
    private Long friendshipId;
    private Long requesterUserId;
    private String requesterNickname;
}

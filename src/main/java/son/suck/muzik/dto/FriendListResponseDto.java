package son.suck.muzik.dto;

import lombok.Getter;
import son.suck.muzik.domain.Users;

@Getter
public class FriendListResponseDto {
    private Long friendshipId;
    private Long friendUserId;
    private String friendNickname;

    public FriendListResponseDto(Long friendshipId, Users friendUser) {
        this.friendshipId = friendshipId;
        this.friendUserId = friendUser.getId();
        this.friendNickname = friendUser.getNickname();

    }
}

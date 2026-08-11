package son.suck.muzik.dto;

import lombok.Getter;
import son.suck.muzik.domain.UserOnlineStatus;
import son.suck.muzik.domain.Users;

@Getter
public class FriendListResponseDto {
    private Long friendshipId;
    private Long friendUserId;
    private String friendNickname;
    private UserOnlineStatus status;

    public FriendListResponseDto(Long friendshipId, Users friendUser, UserOnlineStatus status) {
        this.friendshipId = friendshipId;
        this.friendUserId = friendUser.getId();
        this.friendNickname = friendUser.getNickname();
        this.status = (status != null) ? status : UserOnlineStatus.OFFLINE;

    }
}

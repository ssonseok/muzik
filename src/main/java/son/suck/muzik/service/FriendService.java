package son.suck.muzik.service;

import son.suck.muzik.dto.FriendListResponseDto;
import son.suck.muzik.dto.FriendRequestResponseDto;

import java.util.List;

public interface FriendService {
    // 친구 요청 보내기
    void sendFriendRequest(Long currentUserId, String targetNickname);
    // 받은 친구 요청 목록 조회
    List<FriendRequestResponseDto> getReceivedFriendRequests(Long currentUserId);
    //친추수락
    void acceptFriendRequest(Long currentUserId, Long friendshipId);
    //친구목록 보여줄거(오프라인,게임중,온라인 이런것도 보여줄거임)
    List<FriendListResponseDto> getMyFriendList(Long currentUserId);
}

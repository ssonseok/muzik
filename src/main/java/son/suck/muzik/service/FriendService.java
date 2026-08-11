package son.suck.muzik.service;

public interface FriendService {
    // 친구 요청 보내기
    void sendFriendRequest(Long currentUserId, String targetNickname);
}

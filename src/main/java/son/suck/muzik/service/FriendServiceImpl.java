package son.suck.muzik.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import son.suck.muzik.domain.UserFriend;
import son.suck.muzik.domain.Users;
import son.suck.muzik.repository.UserFriendRepository;
import son.suck.muzik.repository.UsersRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendServiceImpl implements FriendService{

    private final UserFriendRepository userFriendRepository;
    private final UsersRepository usersRepository;

    @Override
    @Transactional
    public void sendFriendRequest(Long currentUserId, String targetNickname) {
        Users currentUser = usersRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. ID: " + currentUserId));

        // 요청받을 상대방(User) 조회
        Users friendUser = usersRepository.findByNickname(targetNickname)
                .orElseThrow(() -> new IllegalArgumentException("해당 닉네임의 유저를 찾을 수 없습니다: " + targetNickname));

        // 자기 자신에게 요청하는지 검증
        if (currentUser.getId().equals(friendUser.getId())) {
            throw new IllegalArgumentException("자기 자신에게는 친구 요청을 보낼 수 없습니다.");
        }

        // 이미 친구 요청을 보냈거나 친구 관계인지 검증
        boolean alreadyExists = userFriendRepository.existsByCurrentUserIdAndFriendUserId(currentUser.getId(), friendUser.getId());
        if (alreadyExists) {
            throw new IllegalArgumentException("이미 친구 요청을 보냈거나 친구 상태입니다.");
        }

        // UserFriend 엔티티 생성 및 저장
        UserFriend userFriend = UserFriend.builder()
                .currentUser(currentUser)
                .friendUser(friendUser)
                .status("REQUESTED")
                .build();

        userFriendRepository.save(userFriend);
    }

    @Override
    @Transactional
    public void acceptFriendRequest(Long currentUserId, Long friendshipId) {
        // 해당 친구 요청 건이 존재하는지, 수락하는 사람(friendUser)이 본인이 맞는지 검증하며 조회
        UserFriend userFriend = userFriendRepository.findByIdAndFriendUserId(friendshipId, currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 본인에게 온 친구 요청이 아닙니다. ID: " + friendshipId));

        //이미 수락된 요청인지 검증
        if ("ACCEPTED".equals(userFriend.getStatus())) {
            throw new IllegalArgumentException("이미 수락 완료된 친구 요청입니다.");
        }

        userFriend.acceptFriend();
    }
}

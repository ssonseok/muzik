package son.suck.muzik.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import son.suck.muzik.domain.UserFriend;
import son.suck.muzik.domain.UserOnlineStatus;
import son.suck.muzik.domain.UserStatus;
import son.suck.muzik.domain.Users;
import son.suck.muzik.dto.FriendListResponseDto;
import son.suck.muzik.repository.UserFriendRepository;
import son.suck.muzik.repository.UsersRepository;

import java.util.List;
import java.util.stream.Collectors;

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


    @Transactional
    @Override
    public void acceptFriendRequest(Long friendshipId, Long currentUserId) {
        UserFriend request = userFriendRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 친구 요청입니다."));

        if (!request.getFriendUser().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("수락 권한이 없습니다.");
        }

        request.updateStatus("ACCEPTED");

        // 반대 방향(요청을 받은 사람) 관계도 새로 생성하여 저장
        boolean reverseExists = userFriendRepository.existsByCurrentUserIdAndFriendUserId(
                request.getFriendUser().getId(),
                request.getCurrentUser().getId()
        );

        if (!reverseExists) {
            UserFriend reverseFriend = UserFriend.builder()
                    .currentUser(request.getFriendUser())
                    .friendUser(request.getCurrentUser())
                    .status("ACCEPTED")
                    .build();

            userFriendRepository.save(reverseFriend);
        }
    }

    @Override
    public List<FriendListResponseDto> getMyFriendList(Long currentUserId) {
        List<UserFriend> acceptedFriends = userFriendRepository.findByCurrentUserIdAndStatus(currentUserId, "ACCEPTED");

        return acceptedFriends.stream()
                .map(uf -> {
                    Users friendUser = uf.getFriendUser();
                    UserStatus userStatus = friendUser.getUserStatus();

                    UserOnlineStatus status = (userStatus != null)
                            ? userStatus.getStatus()
                            : UserOnlineStatus.OFFLINE;

                    return new FriendListResponseDto(uf.getId(), friendUser, status);
                })
                .collect(Collectors.toList());
    }
}

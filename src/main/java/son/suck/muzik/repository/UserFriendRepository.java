package son.suck.muzik.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import son.suck.muzik.domain.UserFriend;

import java.util.List;

public interface UserFriendRepository extends JpaRepository<UserFriend, Long> {

    // 내가 등록한 친구 목록(수락된 상태)을 가져오되,
    // N+1 문제를 막기 위해 친구의 유저 정보(friendUser)와 상태(userStatus)까지 한방에 조인해서 가져옵니다.
    @Query("SELECT uf FROM UserFriend uf " +
            "JOIN FETCH uf.friendUser f " +
            "JOIN FETCH f.userStatus " +
            "WHERE uf.currentUser.id = :currentUserId AND uf.status = :status")
    List<UserFriend> findMyFriendsWithStatus(@Param("currentUserId") Long currentUserId,
                                             @Param("status") String status);
}

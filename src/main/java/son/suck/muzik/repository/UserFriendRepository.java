package son.suck.muzik.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import son.suck.muzik.domain.UserFriend;

import java.util.List;
import java.util.Optional;

public interface UserFriendRepository extends JpaRepository<UserFriend, Long> {

    // 내가 등록한 친구 목록(수락된 상태)을 가져오되,
    // N+1 문제를 막기 위해 친구의 유저 정보(friendUser)와 상태(userStatus)까지 한방에 조인해서 가져옵니다.
    @Query("SELECT uf FROM UserFriend uf " +
            "JOIN FETCH uf.friendUser f " +
            "LEFT JOIN FETCH f.userStatus " +
            "WHERE uf.currentUser.id = :currentUserId AND uf.status = :status")
    List<UserFriend> findByCurrentUserIdAndStatus(@Param("currentUserId") Long currentUserId, @Param("status") String status);

    // 이미 요청을 보냈거나 친구 상태인지 확인
    boolean existsByCurrentUserIdAndFriendUserId(Long currentUserId, Long friendUserId);
    //요청 확인
    Optional<UserFriend> findByIdAndFriendUserId(Long id, Long friendUserId);
    //친구목록
    //List<UserFriend> findByCurrentUserIdAndStatus(Long currentUserId, String status);
    //나에게 들어온 요청 조회
    @Query("SELECT uf FROM UserFriend uf " +
            "JOIN FETCH uf.currentUser c " +
            "WHERE uf.friendUser.id = :friendUserId AND uf.status = :status")
    List<UserFriend> findByFriendUserIdAndStatus(@Param("friendUserId") Long friendUserId, @Param("status") String status);
}

package son.suck.muzik.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import son.suck.muzik.domain.GameHistoryResult;

import java.util.List;

public interface GameHistoryResultRepository extends JpaRepository<GameHistoryResult, Long> {
    // 특정 유저의 개인 전적 기록 최신순 조회
    List<GameHistoryResult> findByUserIdOrderByIdDesc(Long userId);
    //총 플레이 횟수
    long countByUserId(Long userId);
    //1위 횟수
    long countByUserIdAndRanking(Long userId, int ranking);

    //최고 점수 (게임을 한 적이 없으면 null이 올 수 있으므로 Integer)
    @Query("SELECT MAX(r.finalScore) FROM GameHistoryResult r WHERE r.user.id = :userId")
    Integer findMaxScoreByUserId(@Param("userId") Long userId);
}

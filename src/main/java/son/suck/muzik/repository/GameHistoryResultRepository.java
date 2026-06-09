package son.suck.muzik.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import son.suck.muzik.domain.GameHistoryResult;

import java.util.List;

public interface GameHistoryResultRepository extends JpaRepository<GameHistoryResult, Long> {
    // 특정 유저의 개인 전적 기록 최신순 조회
    List<GameHistoryResult> findByUserIdOrderByIdDesc(Long userId);
}

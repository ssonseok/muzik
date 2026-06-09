package son.suck.muzik.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import son.suck.muzik.domain.GameHistory;

public interface GameHistoryRepository extends JpaRepository<GameHistory, Long> {
}

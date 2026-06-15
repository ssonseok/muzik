package son.suck.muzik.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import son.suck.muzik.domain.GameRoom;

import java.util.List;

public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {
    // 대기실에 방을 최신순(ID 역순)으로 정렬해서 조회
    @EntityGraph(attributePaths = {"participants"})
    List<GameRoom> findByRoomStatusOrderByIdDesc(String roomStatus);
}
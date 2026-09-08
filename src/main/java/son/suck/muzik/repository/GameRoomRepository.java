package son.suck.muzik.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import son.suck.muzik.domain.GameRoom;
import son.suck.muzik.domain.RoomType;

import java.util.List;

public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {
    @EntityGraph(attributePaths = {"participants"})
    List<GameRoom> findByRoomStatusOrderByIdDesc(String roomStatus);
    //게임 타입을 추가해서 마피아게임부터 시작(라이어게임이나 추후 게임추가할떄 사용할거임)
    @EntityGraph(attributePaths = {"participants"})
    List<GameRoom> findByRoomTypeAndRoomStatusOrderByIdDesc(RoomType roomType, String roomStatus);
}
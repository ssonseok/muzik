package son.suck.muzik.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import son.suck.muzik.domain.GameParticipant;

import java.util.List;
import java.util.Optional;

public interface GameParticipantRepository extends JpaRepository<GameParticipant, Long> {
    // 특정 게임방에 참여 중인 유저 목록 전체 조회 (실시간 점수 및 인원 갱신용)
    List<GameParticipant> findByGameRoomId(Long roomId);

    // 특정 유저가 이미 다른 방에 들어가 있는지 검증할 때 사용
    Optional<GameParticipant> findByUserId(Long userId);
}

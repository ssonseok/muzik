package son.suck.muzik.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import son.suck.muzik.domain.Users;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByLoginId(String loginId);
    Optional<Users> findByNickname(String nickname);
    boolean existsByLoginId(String loginId);
    boolean existsByNickname(String nickname);
}

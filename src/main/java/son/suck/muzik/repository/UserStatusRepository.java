package son.suck.muzik.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import son.suck.muzik.domain.UserStatus;

import java.util.Optional;

public interface UserStatusRepository extends JpaRepository<UserStatus, Long> {
    Optional<UserStatus> findByUserId(Long userId);
}

package son.suck.muzik.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import son.suck.muzik.domain.UserStatus;

public interface UserStatusRepository extends JpaRepository<UserStatus, Long> {
}

package ma.fstg.security.repositories;

import java.util.Optional;
import ma.fstg.security.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * CRUD repository for the application users.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
}

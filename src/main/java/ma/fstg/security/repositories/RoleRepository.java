package ma.fstg.security.repositories;

import java.util.Optional;
import ma.fstg.security.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * CRUD repository for the roles.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);
}

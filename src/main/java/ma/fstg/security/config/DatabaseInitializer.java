package ma.fstg.security.config;

import java.util.List;
import ma.fstg.security.entities.Role;
import ma.fstg.security.entities.User;
import ma.fstg.security.repositories.RoleRepository;
import ma.fstg.security.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Inserts the default roles and users on startup.
 * Skipped during the tests so each test controls its own data.
 */
@Configuration
@Profile("!test")
public class DatabaseInitializer {

    @Bean
    CommandLineRunner init(RoleRepository roleRepository,
                           UserRepository userRepository,
                           BCryptPasswordEncoder encoder) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            Role adminRole = roleRepository.save(new Role(null, "ROLE_ADMIN"));
            Role userRole = roleRepository.save(new Role(null, "ROLE_USER"));

            User admin = new User(null, "admin", encoder.encode("1234"), true, List.of(adminRole, userRole));
            User user = new User(null, "user", encoder.encode("1111"), true, List.of(userRole));

            userRepository.saveAll(List.of(admin, user));

            System.out.println("Default accounts created:");
            userRepository.findAll().forEach(u -> System.out.println(
                    "  " + u.getUsername() + " -> " + u.getRoles().stream()
                            .map(Role::getName).toList()));
        };
    }
}

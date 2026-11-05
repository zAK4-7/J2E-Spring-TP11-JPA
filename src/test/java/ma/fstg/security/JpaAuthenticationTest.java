package ma.fstg.security;

import java.util.List;
import ma.fstg.security.entities.Role;
import ma.fstg.security.entities.User;
import ma.fstg.security.repositories.RoleRepository;
import ma.fstg.security.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ma.fstg.security.services.CustomUserDetailsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Verifies the whole database backed authentication: entity mapping, role loading
 * and the login against the persisted users.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JpaAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void seed() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role adminRole = roleRepository.save(new Role(null, "ROLE_ADMIN"));
        Role userRole = roleRepository.save(new Role(null, "ROLE_USER"));

        userRepository.save(new User(null, "admin", passwordEncoder.encode("1234"), true, List.of(adminRole, userRole)));
        userRepository.save(new User(null, "user", passwordEncoder.encode("1111"), true, List.of(userRole)));
    }

    @Test
    void usersAndRolesShouldBePersistedAndLinked() {
        User admin = userRepository.findByUsername("admin").orElseThrow();

        assertEquals(2, admin.getRoles().size());
        assertTrue(admin.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN")));
    }

    @Test
    void passwordsShouldBeStoredAsBcryptHashes() {
        User user = userRepository.findByUsername("user").orElseThrow();

        assertTrue(user.getPassword().startsWith("$2"));
        assertTrue(passwordEncoder.matches("1111", user.getPassword()));
    }

    @Test
    void userDetailsServiceShouldMapRolesToAuthorities() {
        UserDetails details = userDetailsService.loadUserByUsername("admin");

        assertEquals(2, details.getAuthorities().size());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void unknownUserShouldRaiseUsernameNotFound() {
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("ghost"));
    }

    @Test
    void adminShouldAuthenticateAndReachTheAdminArea() throws Exception {
        mockMvc.perform(formLogin("/login").user("admin").password("1234"))
                .andExpect(authenticated().withUsername("admin"));

        mockMvc.perform(get("/admin/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-dashboard"));
    }

    @Test
    void wrongPasswordShouldNotAuthenticate() throws Exception {
        mockMvc.perform(formLogin("/login").user("admin").password("nope"))
                .andExpect(unauthenticated());
    }

    @Test
    void simpleUserShouldNotReachTheAdminArea() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void inactiveUserFlagIsStoredOnTheEntity() {
        User user = userRepository.findByUsername("user").orElseThrow();

        assertTrue(user.isActive());
    }
}

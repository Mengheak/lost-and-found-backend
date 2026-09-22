package com.group5.lostandfoundjava.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group5.lostandfoundjava.config.AdminProperties;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.Role;
import com.group5.lostandfoundjava.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AdminBootstrapTest {

    private static final String STRONG_PASSWORD = "a-long-admin-password";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ApplicationArguments args = mock(ApplicationArguments.class);

    private AdminBootstrap bootstrap(String email, String password, String name, boolean resetPassword) {
        return new AdminBootstrap(
                userRepository, passwordEncoder, new AdminProperties(email, password, name, resetPassword));
    }

    @Test
    void createsAdminOnlyFromExplicitCredentials() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(saved.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        bootstrap("admin@example.com", STRONG_PASSWORD, "Initial Admin", false).run(args);

        assertEquals("admin@example.com", saved.getValue().getEmail());
        assertEquals("Initial Admin", saved.getValue().getName());
        assertEquals(Role.ADMIN, saved.getValue().getRole());
        assertTrue(passwordEncoder.matches(STRONG_PASSWORD, saved.getValue().getPasswordHash()));
    }

    @Test
    void normalisesConfiguredEmail() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(saved.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        bootstrap("  Admin@Example.COM  ", STRONG_PASSWORD, "Administrator", false).run(args);

        assertEquals("admin@example.com", saved.getValue().getEmail());
    }

    @Test
    void refusesToPromoteExistingRegularAccount() {
        User existing = user(Role.USER, "existing-hash");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class,
                () -> bootstrap("admin@example.com", STRONG_PASSWORD, "Administrator", false).run(args));

        assertEquals(Role.USER, existing.getRole());
        assertEquals("existing-hash", existing.getPasswordHash());
        verify(userRepository, never()).save(any());
    }

    @Test
    void existingAdminIsUnchangedByDefault() {
        User existing = user(Role.ADMIN, passwordEncoder.encode("the-existing-password"));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(existing));

        bootstrap("admin@example.com", STRONG_PASSWORD, "Administrator", false).run(args);

        assertTrue(passwordEncoder.matches("the-existing-password", existing.getPasswordHash()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void refusesToRunWithRetiredDefaultPasswordStillActive() {
        User existing = user(Role.ADMIN, passwordEncoder.encode("12345678"));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class,
                () -> bootstrap("admin@example.com", STRONG_PASSWORD, "Administrator", false).run(args));

        verify(userRepository, never()).save(any());
    }

    @Test
    void explicitResetChangesOnlyExistingAdminPassword() {
        User existing = user(Role.ADMIN, passwordEncoder.encode("the-existing-password"));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        bootstrap("admin@example.com", STRONG_PASSWORD, "Administrator", true).run(args);

        assertTrue(passwordEncoder.matches(STRONG_PASSWORD, existing.getPasswordHash()));
        verify(userRepository).save(existing);
    }

    @Test
    void disabledWhenEmailAndPasswordAreBothEmpty() {
        bootstrap("", "", "Administrator", false).run(args);

        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsPartialConfiguration() {
        assertThrows(IllegalStateException.class,
                () -> bootstrap("admin@example.com", "", "Administrator", false).run(args));
        assertThrows(IllegalStateException.class,
                () -> bootstrap("", STRONG_PASSWORD, "Administrator", false).run(args));
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void rejectsWeakAdminPassword() {
        assertThrows(IllegalStateException.class,
                () -> bootstrap("admin@example.com", "12345678", "Administrator", false).run(args));
        verify(userRepository, never()).findByEmail(any());
    }

    private User user(Role role, String passwordHash) {
        return new User("Existing", "admin@example.com", null, passwordHash, role);
    }
}

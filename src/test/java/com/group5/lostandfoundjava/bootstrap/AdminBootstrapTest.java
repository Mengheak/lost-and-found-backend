package com.group5.lostandfoundjava.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * The default-admin rules, and above all the promise that an existing account's password is never
 * silently overwritten.
 */
class AdminBootstrapTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ApplicationArguments args = mock(ApplicationArguments.class);

    private AdminBootstrap bootstrap(String email, String password, String name, boolean resetPassword) {
        return new AdminBootstrap(
                userRepository, passwordEncoder, new AdminProperties(email, password, name, resetPassword));
    }

    private AdminBootstrap bootstrap() {
        return bootstrap("admin@example.com", "12345678", "Administrator", false);
    }

    @Test
    @DisplayName("creates the admin account when it does not exist yet")
    void createsAdminWhenMissing() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(saved.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        bootstrap().run(args);

        assertEquals("admin@example.com", saved.getValue().getEmail());
        assertEquals("Administrator", saved.getValue().getName());
        assertEquals(Role.ADMIN, saved.getValue().getRole());
        assertTrue(passwordEncoder.matches("12345678", saved.getValue().getPasswordHash()));
    }

    @Test
    @DisplayName("lowercases and trims the configured email")
    void normalisesConfiguredEmail() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(saved.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        bootstrap("  Admin@Example.COM  ", "12345678", "Administrator", false).run(args);

        assertEquals("admin@example.com", saved.getValue().getEmail());
    }

    @Test
    @DisplayName("promotes an existing account instead of creating a second one")
    void promotesExistingAccount() {
        User existing = user(Role.USER, "hashed");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        bootstrap().run(args);

        assertEquals(Role.ADMIN, existing.getRole());
    }

    @Test
    @DisplayName("never overwrites the password of an existing account")
    void neverOverwritesExistingPassword() {
        User existing = user(Role.USER, passwordEncoder.encode("the-real-password"));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        bootstrap().run(args);

        assertTrue(passwordEncoder.matches("the-real-password", existing.getPasswordHash()));
    }

    @Test
    @DisplayName("resets an existing password when the reset flag is on")
    void resetFlagResetsPassword() {
        User existing = user(Role.ADMIN, passwordEncoder.encode("forgotten"));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        bootstrap("admin@example.com", "12345678", "Administrator", true).run(args);

        assertTrue(passwordEncoder.matches("12345678", existing.getPasswordHash()));
    }

    @Test
    @DisplayName("reset flag also promotes the account in the same pass")
    void resetFlagAlsoPromotes() {
        User existing = user(Role.USER, passwordEncoder.encode("forgotten"));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        bootstrap("admin@example.com", "12345678", "Administrator", true).run(args);

        assertEquals(Role.ADMIN, existing.getRole());
        assertTrue(passwordEncoder.matches("12345678", existing.getPasswordHash()));
    }

    @Test
    @DisplayName("reset flag with no configured password leaves the hash alone")
    void resetFlagWithoutPasswordDoesNothing() {
        User existing = user(Role.ADMIN, passwordEncoder.encode("the-real-password"));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(existing));

        bootstrap("admin@example.com", "", "Administrator", true).run(args);

        assertTrue(passwordEncoder.matches("the-real-password", existing.getPasswordHash()));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("does nothing when the account is already an admin")
    void doesNothingWhenAlreadyAdmin() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user(Role.ADMIN, "hashed")));

        bootstrap().run(args);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("does nothing when no email is configured")
    void doesNothingWithoutConfiguredEmail() {
        bootstrap("", "12345678", "Administrator", false).run(args);

        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("does not create an account when no password is configured")
    void doesNotCreateAccountWithoutPassword() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());

        bootstrap("admin@example.com", "", "Administrator", false).run(args);

        verify(userRepository, never()).save(any());
    }

    private User user(Role role, String passwordHash) {
        return new User("Existing", "admin@example.com", null, passwordHash, role);
    }
}

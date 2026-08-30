package com.group5.lostandfoundjava.bootstrap;

import com.group5.lostandfoundjava.config.AdminProperties;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.Role;
import com.group5.lostandfoundjava.repository.UserRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Makes sure there is always one administrator to sign in with.
 *
 * <p>An {@link ApplicationRunner} runs once, right after startup. On every boot this checks the
 * account named by {@code app.admin.email}:
 *
 * <ul>
 *   <li>missing — it is created with the configured password
 *   <li>exists but is a regular user — it is promoted to {@code ADMIN}
 *   <li>exists already as an admin — nothing happens
 * </ul>
 *
 * <p>An existing account's password is never overwritten, so pointing this at a real user's email
 * cannot hand their account to whoever knows the configured password. The one exception is the
 * deliberate {@code resetPassword} escape hatch for when everybody is locked out.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties properties;

    public AdminBootstrap(
            UserRepository userRepository, PasswordEncoder passwordEncoder, AdminProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = properties.email().trim().toLowerCase();
        if (email.isEmpty()) {
            return; // The feature is switched off.
        }

        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            updateExisting(existing.get(), email);
            return;
        }

        if (properties.password().isEmpty()) {
            log.warn("app.admin.email is '{}' but no account exists and no password is set; skipping", email);
            return;
        }

        userRepository.save(new User(
                properties.name().trim(), email, null, passwordEncoder.encode(properties.password()), Role.ADMIN));
        log.info("Created default ADMIN account '{}' — change its password after first login", email);
    }

    private void updateExisting(User user, String email) {
        boolean changed = false;

        if (user.getRole() != Role.ADMIN) {
            user.setRole(Role.ADMIN);
            changed = true;
            log.info("Promoted existing account '{}' to ADMIN", email);
        }

        if (properties.resetPassword() && !properties.password().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(properties.password()));
            changed = true;
            log.warn(
                    "Reset the password of '{}' from app.admin.password — "
                            + "turn ADMIN_RESET_PASSWORD off again once you are back in",
                    email);
        }

        // Only write when something actually changed, so a normal restart touches nothing.
        if (changed) {
            userRepository.save(user);
        }
    }
}

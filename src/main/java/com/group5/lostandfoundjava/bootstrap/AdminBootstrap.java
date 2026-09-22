package com.group5.lostandfoundjava.bootstrap;

import com.group5.lostandfoundjava.config.AdminProperties;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.Role;
import com.group5.lostandfoundjava.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Creates an initial administrator only when explicit, safe credentials are supplied.
@Component
@Slf4j
public class AdminBootstrap implements ApplicationRunner {

    private static final int MINIMUM_PASSWORD_LENGTH = 12;
    private static final String RETIRED_INSECURE_PASSWORD = "12345678";

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
        String password = properties.password();
        if (email.isEmpty() && password.isEmpty()) {
            return;
        }
        if (email.isEmpty() || password.isEmpty()) {
            throw new IllegalStateException("ADMIN_EMAIL and ADMIN_PASSWORD must either both be set or both be empty");
        }
        if (password.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalStateException("ADMIN_PASSWORD must contain at least 12 characters");
        }

        userRepository.findByEmail(email).ifPresentOrElse(
                user -> updateExistingAdmin(user, email, password),
                () -> createAdmin(email, password));
    }

    private void updateExistingAdmin(User user, String email, String password) {
        if (user.getRole() != Role.ADMIN) {
            throw new IllegalStateException(
                    "ADMIN_EMAIL belongs to an existing non-admin account; refusing to promote it automatically");
        }
        if (!properties.resetPassword()
                && passwordEncoder.matches(RETIRED_INSECURE_PASSWORD, user.getPasswordHash())) {
            throw new IllegalStateException(
                    "Existing ADMIN uses the retired insecure default password; configure a new password "
                            + "and set ADMIN_RESET_PASSWORD=true for one startup");
        }
        if (properties.resetPassword()) {
            user.setPasswordHash(passwordEncoder.encode(password));
            userRepository.save(user);
            log.warn(
                    "Reset the password of existing ADMIN '{}' from app.admin.password; "
                            + "turn ADMIN_RESET_PASSWORD off again once you are back in",
                    email);
        }
    }

    private void createAdmin(String email, String password) {
        String configuredName = properties.name().trim();
        String name = configuredName.isEmpty() ? "Administrator" : configuredName;
        userRepository.save(new User(name, email, null, passwordEncoder.encode(password), Role.ADMIN));
        log.info("Created explicitly configured initial ADMIN account '{}'", email);
    }
}

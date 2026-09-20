package com.suresh.sms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.suresh.sms.dto.RegisterRequest;
import com.suresh.sms.entity.Role;
import com.suresh.sms.service.UserService;

/**
 * Creates the first ADMIN account at startup, from environment variables.
 *
 * <p>Runs only when ADMIN_PASSWORD is set, and never overwrites an existing
 * account, so it is safe to leave enabled across restarts.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserService userService;
    private final String username;
    private final String email;
    private final String password;

    public AdminSeeder(
            UserService userService,
            @Value("${app.admin.username:admin}") String username,
            @Value("${app.admin.email:admin@example.com}") String email,
            @Value("${app.admin.password:}") String password) {

        this.userService = userService;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {

        if (password == null || password.isBlank()) {
            log.info("ADMIN_PASSWORD not set - skipping admin seeding");
            return;
        }

        if (userService.usernameExists(username)) {
            log.info("Admin user '{}' already exists - skipping seeding", username);
            return;
        }

        userService.createUser(new RegisterRequest(username, email, password), Role.ADMIN);
        log.info("Seeded admin user '{}'", username);
    }
}

package com.group5.lostandfoundjava.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Everything under {@code app.admin}: the default administrator ensured on every startup by
 * {@link com.group5.lostandfoundjava.bootstrap.AdminBootstrap}.
 *
 * @param email leave empty to disable the whole mechanism
 * @param password only used when the account is created, unless {@code resetPassword} is on
 * @param name display name for a freshly created admin
 * @param resetPassword turn on for a single boot to force the password back to {@code password};
 *     an emergency way back in, not something to leave enabled
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        @DefaultValue("") String email,
        @DefaultValue("") String password,
        @DefaultValue("Administrator") String name,
        @DefaultValue("false") boolean resetPassword) {}

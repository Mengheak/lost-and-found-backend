package com.group5.lostandfoundjava.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// Everything under app.admin: the default administrator ensured on every startup
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        @DefaultValue("") String email,
        @DefaultValue("") String password,
        @DefaultValue("Administrator") String name,
        @DefaultValue("false") boolean resetPassword) {}

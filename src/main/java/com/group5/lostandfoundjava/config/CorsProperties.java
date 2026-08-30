package com.group5.lostandfoundjava.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Everything under {@code app.cors}.
 *
 * @param allowedOrigins browser origins allowed to call this API. The default covers the Angular
 *     dev server; set the real host in production
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        @DefaultValue({"http://localhost:4200", "http://localhost:4300"}) List<String> allowedOrigins) {}

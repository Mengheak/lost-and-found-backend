package com.group5.lostandfoundjava.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.group5.lostandfoundjava.service.PushSender;
import com.group5.lostandfoundjava.service.impl.FcmPushSender;
import com.group5.lostandfoundjava.service.impl.NoopPushSender;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chooses which {@link PushSender} the application uses.
 *
 * <p>Push notifications are optional: without a Firebase credentials file the app starts normally
 * with a no-op sender. Picking the implementation here means no other class ever has to ask whether
 * push is switched on.
 */
@Configuration
public class PushConfig {

    private static final Logger log = LoggerFactory.getLogger(PushConfig.class);

    @Bean
    public PushSender pushSender(@Value("${app.fcm.credentials-path:}") String credentialsPath) throws IOException {
        if (credentialsPath == null || credentialsPath.isBlank() || !Files.exists(Path.of(credentialsPath))) {
            log.info("FCM credentials not configured — push notifications are disabled");
            return new NoopPushSender();
        }

        FirebaseOptions options;
        try (FileInputStream credentials = new FileInputStream(credentialsPath)) {
            options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentials))
                    .build();
        }
        FirebaseApp app =
                FirebaseApp.getApps().isEmpty() ? FirebaseApp.initializeApp(options) : FirebaseApp.getInstance();

        log.info("FCM initialized — push notifications are enabled");
        return new FcmPushSender(FirebaseMessaging.getInstance(app));
    }
}

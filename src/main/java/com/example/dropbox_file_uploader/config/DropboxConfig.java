package com.example.dropbox_file_uploader.config;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DropboxConfig {
    private static final Logger logger = LoggerFactory.getLogger(DropboxConfig.class);

    @Value("${dropbox.access-token}")
    private String accessToken;

    /**
     * Creates and configures a Dropbox client instance.
     * <p>
     * This method initializes a Dropbox client with specific configuration settings
     * including auto-retry capability and Bulgarian locale. The client uses the access
     * token specified in the application properties to authenticate with the Dropbox API.
     * </p>
     * 
     * @return A configured {@link DbxClientV2} instance ready to interact with the Dropbox API
     */
    @Bean
    public DbxClientV2 dropboxClient() {
        logger.info("Initializing Dropbox client");

        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox-file-uploader")
                .withAutoRetryEnabled(3)
                .withUserLocale("bg_BG")
                .build();

        return new DbxClientV2(config, accessToken);
    }
}
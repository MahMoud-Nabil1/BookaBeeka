package com.system.booking.modules.media.internal.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Binds {@code cloudinary.*} properties and exposes a configured {@link Cloudinary} bean.
 *
 * <p>Credentials are read from environment variables via application.properties —
 * never hardcoded.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "cloudinary")
@Getter
@Setter
public class CloudinaryConfig {

    private String cloudName;
    private String apiKey;
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }
}

package com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The technical profiles configured for the thinning prescription, one per olive variety
 * ({@code viora.thinning.profiles.<variety>.*}). There are no defaults: a variety without a complete,
 * coherent profile has no prescription, instead of one built on an invented number.
 */
@ConfigurationProperties(prefix = "viora.thinning")
public class ThinningProfiles {

    private static final Logger LOG = LoggerFactory.getLogger(ThinningProfiles.class);

    private Map<String, Profile> profiles = new HashMap<>();

    public Map<String, Profile> getProfiles() {
        return profiles;
    }

    public void setProfiles(Map<String, Profile> profiles) {
        this.profiles = profiles == null ? new HashMap<>() : profiles;
    }

    /**
     * Finds the profile of a variety.
     *
     * @param variety olive variety name, in any case
     * @return the profile, or empty when none is configured or the configured one is incomplete
     */
    public Optional<ThinningProfile> forVariety(String variety) {
        if (variety == null) {
            return Optional.empty();
        }
        return profiles.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(variety))
                .findFirst()
                .flatMap(entry -> entry.getValue().toProfile(variety.toUpperCase(java.util.Locale.ROOT)));
    }

    /** Mutable holder the configuration binds into. */
    public static class Profile {
        private Double targetFruitsPerShoot;
        private Integer windowOpensDaysAfterBloom;
        private Integer windowClosesDaysAfterBloom;
        private String status;
        private String version;
        private String source;
        private String approvedBy;

        Optional<ThinningProfile> toProfile(String variety) {
            if (targetFruitsPerShoot == null || windowOpensDaysAfterBloom == null || windowClosesDaysAfterBloom == null) {
                LOG.warn("Thinning profile of {} ignored: target and window must all be set", variety);
                return Optional.empty();
            }
            try {
                return Optional.of(new ThinningProfile(variety, targetFruitsPerShoot, windowOpensDaysAfterBloom,
                        windowClosesDaysAfterBloom, status, version, source, approvedBy));
            } catch (IllegalArgumentException exception) {
                LOG.warn("Thinning profile of {} ignored: {}", variety, exception.getMessage());
                return Optional.empty();
            }
        }

        public Double getTargetFruitsPerShoot() {
            return targetFruitsPerShoot;
        }

        public void setTargetFruitsPerShoot(Double targetFruitsPerShoot) {
            this.targetFruitsPerShoot = targetFruitsPerShoot;
        }

        public Integer getWindowOpensDaysAfterBloom() {
            return windowOpensDaysAfterBloom;
        }

        public void setWindowOpensDaysAfterBloom(Integer windowOpensDaysAfterBloom) {
            this.windowOpensDaysAfterBloom = windowOpensDaysAfterBloom;
        }

        public Integer getWindowClosesDaysAfterBloom() {
            return windowClosesDaysAfterBloom;
        }

        public void setWindowClosesDaysAfterBloom(Integer windowClosesDaysAfterBloom) {
            this.windowClosesDaysAfterBloom = windowClosesDaysAfterBloom;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getApprovedBy() {
            return approvedBy;
        }

        public void setApprovedBy(String approvedBy) {
            this.approvedBy = approvedBy;
        }
    }
}

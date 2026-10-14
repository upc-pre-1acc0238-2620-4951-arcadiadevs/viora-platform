package com.arcadiadevs.viora.platform.profiles.application.acl;

import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.profiles.domain.repositories.ProfileRepository;
import com.arcadiadevs.viora.platform.profiles.interfaces.acl.ProfileContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementation of {@link ProfileContextFacade} translating inter-context queries.
 */
@Service
@Transactional(readOnly = true)
public class ProfileContextFacadeImpl implements ProfileContextFacade {

    private final ProfileRepository profileRepository;

    public ProfileContextFacadeImpl(ProfileRepository profileRepository) {
        if (profileRepository == null) {
            throw new IllegalArgumentException("profile.repository.null");
        }
        this.profileRepository = profileRepository;
    }

    @Override
    public boolean existsProfileByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        try {
            return profileRepository.existsByUserId(new UserId(userId));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public Optional<String> findFullNameByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        try {
            return profileRepository.findByUserId(new UserId(userId))
                    .map(profile -> profile.snapshot().fullName().value());
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> findPhoneNumberByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        try {
            return profileRepository.findByUserId(new UserId(userId))
                    .map(profile -> profile.snapshot().phoneNumber().value());
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}

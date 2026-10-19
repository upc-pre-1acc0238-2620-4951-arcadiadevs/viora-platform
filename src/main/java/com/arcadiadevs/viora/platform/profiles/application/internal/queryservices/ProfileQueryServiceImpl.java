package com.arcadiadevs.viora.platform.profiles.application.internal.queryservices;

import com.arcadiadevs.viora.platform.profiles.application.queryservices.ProfileQueryService;
import com.arcadiadevs.viora.platform.profiles.domain.model.aggregates.Profile;
import com.arcadiadevs.viora.platform.profiles.domain.model.queries.GetProfileByIdQuery;
import com.arcadiadevs.viora.platform.profiles.domain.model.queries.GetProfileByUserIdQuery;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.profiles.domain.repositories.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Read-only application service implementation for querying profiles.
 */
@Service
@Transactional(readOnly = true)
public class ProfileQueryServiceImpl implements ProfileQueryService {

    private final ProfileRepository profileRepository;

    public ProfileQueryServiceImpl(ProfileRepository profileRepository) {
        if (profileRepository == null) {
            throw new IllegalArgumentException("profile.repository.null");
        }
        this.profileRepository = profileRepository;
    }

    @Override
    public Optional<Profile> handle(GetProfileByIdQuery query) {
        if (query == null) {
            return Optional.empty();
        }
        return profileRepository.findById(query.profileId());
    }

    @Override
    public Result<Profile, ApplicationError> handle(GetProfileByUserIdQuery query) {
        if (query == null) {
            return Result.failure(ApplicationError.validationError("query", "profile.query.null"));
        }
        return profileRepository.findByUserId(query.userId())
                .<Result<Profile, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(ApplicationError.notFound("profile", query.userId().userId())));
    }
}

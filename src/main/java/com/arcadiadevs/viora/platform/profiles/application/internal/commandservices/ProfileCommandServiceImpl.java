package com.arcadiadevs.viora.platform.profiles.application.internal.commandservices;

import com.arcadiadevs.viora.platform.profiles.application.commandservices.ProfileCommandService;
import com.arcadiadevs.viora.platform.profiles.application.internal.outboundservices.acl.ExternalIamService;
import com.arcadiadevs.viora.platform.profiles.domain.model.aggregates.Profile;
import com.arcadiadevs.viora.platform.profiles.domain.model.commands.CreateProfileCommand;
import com.arcadiadevs.viora.platform.profiles.domain.model.commands.DeleteProfileCommand;
import com.arcadiadevs.viora.platform.profiles.domain.model.commands.UpdateContactProfileCommand;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.CountryCode;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.FullName;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.PhoneNumber;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.profiles.domain.repositories.ProfileRepository;
import com.arcadiadevs.viora.platform.profiles.domain.model.ports.PhoneNumberValidator;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service implementation orchestrating profile mutation use cases.
 */
@Service
@Transactional
public class ProfileCommandServiceImpl implements ProfileCommandService {

    private final ProfileRepository profileRepository;
    private final ExternalIamService externalIamService;
    private final PhoneNumberValidator phoneNumberValidator;
    private final ApplicationEventPublisher eventPublisher;

    public ProfileCommandServiceImpl(
            ProfileRepository profileRepository,
            ExternalIamService externalIamService,
            PhoneNumberValidator phoneNumberValidator,
            ApplicationEventPublisher eventPublisher
    ) {
        if (profileRepository == null) {
            throw new IllegalArgumentException("profile.repository.null");
        }
        if (externalIamService == null) {
            throw new IllegalArgumentException("profile.iam_service.null");
        }
        if (phoneNumberValidator == null) {
            throw new IllegalArgumentException("profile.phone_service.null");
        }
        if (eventPublisher == null) {
            throw new IllegalArgumentException("profile.event_publisher.null");
        }
        this.profileRepository = profileRepository;
        this.externalIamService = externalIamService;
        this.phoneNumberValidator = phoneNumberValidator;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Result<String, ApplicationError> handle(CreateProfileCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "profile.command.null"));
        }
        try {
            var userId = new UserId(command.userId());
            if (!externalIamService.existsUser(userId)) {
                return Result.failure(ApplicationError.notFound("user", command.userId()));
            }

            if (profileRepository.existsByUserId(userId)) {
                return Result.failure(ApplicationError.conflict("profile", "profile.user_id.already_exists"));
            }

            var fullName = new FullName(command.fullName());
            var country = new CountryCode(command.country());
            String formattedPhone = phoneNumberValidator.formatToE164(command.phoneNumber(), command.country());
            var phoneNumber = new PhoneNumber(formattedPhone);

            var profile = Profile.create(userId, fullName, country, phoneNumber);
            var savedProfile = profileRepository.save(profile);

            for (var event : profile.domainEvents()) {
                eventPublisher.publishEvent(event);
            }
            profile.clearDomainEvents();

            return Result.success(savedProfile.snapshot().id().profileId());
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }

    @Override
    public Result<Profile, ApplicationError> handle(UpdateContactProfileCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "profile.command.null"));
        }
        try {
            var userId = new UserId(command.userId());
            var optionalProfile = profileRepository.findByUserId(userId);
            if (optionalProfile.isEmpty()) {
                return Result.failure(ApplicationError.notFound("profile", command.userId()));
            }

            var profile = optionalProfile.get();
            var fullName = new FullName(command.fullName());
            var country = new CountryCode(command.country());
            String formattedPhone = phoneNumberValidator.formatToE164(command.phoneNumber(), command.country());
            var phoneNumber = new PhoneNumber(formattedPhone);

            profile.updateContact(fullName, country, phoneNumber);
            var savedProfile = profileRepository.save(profile);

            for (var event : profile.domainEvents()) {
                eventPublisher.publishEvent(event);
            }
            profile.clearDomainEvents();

            return Result.success(savedProfile);
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }

    @Override
    public Result<Boolean, ApplicationError> handle(DeleteProfileCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "profile.command.null"));
        }
        try {
            var optionalProfile = profileRepository.findByUserId(new UserId(command.userId()));
            if (optionalProfile.isEmpty()) {
                return Result.success(false);
            }
            profileRepository.delete(optionalProfile.get());
            return Result.success(true);
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }
}

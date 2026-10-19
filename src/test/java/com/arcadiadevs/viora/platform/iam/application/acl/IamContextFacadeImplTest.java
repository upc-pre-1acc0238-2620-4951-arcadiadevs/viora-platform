package com.arcadiadevs.viora.platform.iam.application.acl;

import com.arcadiadevs.viora.platform.iam.domain.model.UserAccountFixtures;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserRole;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("IamContextFacade Open Host Service Unit Tests")
class IamContextFacadeImplTest {

    private final UserAccountRepository repository = Mockito.mock(UserAccountRepository.class);
    private final IamContextFacadeImpl facade = new IamContextFacadeImpl(repository);

    @Test
    @DisplayName("Counts a pending account as existing but not as active")
    void pendingAccountExistsButIsNotActive() {
        var pending = UserAccountFixtures.pendingAccount("productor@viora.ag", "pw:Password123");
        when(repository.findById(any(UserId.class))).thenReturn(Optional.of(pending));
        var userId = pending.snapshot().id().userId();

        assertThat(facade.existsUser(userId)).isTrue();
        assertThat(facade.existsActiveUser(userId)).isFalse();
    }

    @Test
    @DisplayName("Counts a verified account as existing and active")
    void verifiedAccountIsActive() {
        var active = UserAccountFixtures.activeAccount("productor@viora.ag", "pw:Password123", UserRole.ROLE_PRODUCER);
        when(repository.findById(any(UserId.class))).thenReturn(Optional.of(active));
        var userId = active.snapshot().id().userId();

        assertThat(facade.existsUser(userId)).isTrue();
        assertThat(facade.existsActiveUser(userId)).isTrue();
    }

    @Test
    @DisplayName("Answers false for unknown or malformed identifiers")
    void unknownOrMalformedIdentifiers() {
        when(repository.findById(any(UserId.class))).thenReturn(Optional.empty());

        assertThat(facade.existsUser(UUID.randomUUID().toString())).isFalse();
        assertThat(facade.existsActiveUser("not-a-uuid")).isFalse();
        assertThat(facade.existsUser(" ")).isFalse();
    }
}

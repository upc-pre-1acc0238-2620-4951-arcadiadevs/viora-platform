package com.arcadiadevs.viora.platform.subscription.infrastructure;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.arcadiadevs.viora.platform.iam.interfaces.acl.IamContextFacade;
import com.arcadiadevs.viora.platform.profiles.interfaces.acl.ProfileContextFacade;
import com.arcadiadevs.viora.platform.subscription.infrastructure.acl.ProfileReadinessAdapter;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ProfileReadinessAdapterTest {
    @Test
    void rejectsUnknownAndNonproducerIdentitiesEvenWithProfile() {
        var identity = mock(IamContextFacade.class);
        var profiles = mock(ProfileContextFacade.class);
        var adapter = new ProfileReadinessAdapter(profiles, identity);
        when(profiles.existsProfileByUserId("fixture")).thenReturn(true);
        when(identity.findUserRole("fixture")).thenReturn(Optional.empty());
        assertFalse(adapter.isReady("fixture"));
        when(identity.findUserRole("fixture")).thenReturn(Optional.of("ROLE_MANAGER"));
        assertFalse(adapter.isReady("fixture"));
        when(identity.findUserRole("fixture")).thenReturn(Optional.of("ROLE_PRODUCER"));
        assertTrue(adapter.isReady("fixture"));
    }
}

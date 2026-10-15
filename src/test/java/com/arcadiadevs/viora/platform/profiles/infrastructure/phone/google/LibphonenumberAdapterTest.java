package com.arcadiadevs.viora.platform.profiles.infrastructure.phone.google;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LibphonenumberAdapter Infrastructure Adapter Unit Tests")
class LibphonenumberAdapterTest {

    private final LibphonenumberAdapter adapter = new LibphonenumberAdapter();

    @Test
    @DisplayName("Should successfully normalize national and international numbers into E.164")
    void shouldFormatValidNumbersToE164() {
        // Peru local number with country region
        String peruE164 = adapter.formatToE164("987654321", "PE");
        assertThat(peruE164).isEqualTo("+51987654321");

        // Spain number with international prefix
        String spainE164 = adapter.formatToE164("+34612345678", "ES");
        assertThat(spainE164).isEqualTo("+34612345678");

        // International format with Chilean country code
        String chileE164 = adapter.formatToE164("+56987654321", "CL");
        assertThat(chileE164).isEqualTo("+56987654321");
    }

    @Test
    @DisplayName("Should reject invalid or malformed telephone numbers")
    void shouldRejectInvalidNumbers() {
        assertThatThrownBy(() -> adapter.formatToE164("123", "PE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("profile.phone.invalid_e164");

        assertThatThrownBy(() -> adapter.formatToE164("", "PE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("profile.phone.null_or_empty");
    }
}

package com.arcadiadevs.viora.platform.subscription.infrastructure.codes.securerandom;

import com.arcadiadevs.viora.platform.subscription.application.ports.InvitationCodeGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * {@link InvitationCodeGenerator} drawing the four characters from letters and digits that cannot be mistaken for one
 * another when read aloud or copied by hand: no {@code 0}, {@code O}, {@code 1}, {@code I} or {@code L}.
 */
@Component
public class SecureRandomInvitationCodeGenerator implements InvitationCodeGenerator {

    static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 4;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String next() {
        var code = new StringBuilder("COOP-");
        for (var index = 0; index < LENGTH; index++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}

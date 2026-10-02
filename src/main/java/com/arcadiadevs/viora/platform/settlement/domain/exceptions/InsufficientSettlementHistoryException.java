package com.arcadiadevs.viora.platform.settlement.domain.exceptions;

import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.ResourceConflictException;

/**
 * Raised when the frozen curve of a campaign has no managed alternation index, that is, fewer than three
 * consecutive settled campaigns back it. It is currently reported as a conflict; see ADR-002 section 8 for the
 * pending decision on its error code and HTTP status.
 */
public class InsufficientSettlementHistoryException extends ResourceConflictException {

    /**
     * @param message i18n key describing the conflict
     */
    public InsufficientSettlementHistoryException(String message) {
        super(message);
    }
}

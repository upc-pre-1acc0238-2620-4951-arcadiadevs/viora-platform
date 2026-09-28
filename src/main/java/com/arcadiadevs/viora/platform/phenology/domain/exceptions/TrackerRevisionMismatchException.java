package com.arcadiadevs.viora.platform.phenology.domain.exceptions;

import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.TrackerId;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;

/**
 * Domain exception thrown when an optimistic concurrency revision mismatch occurs on a phenology tracker.
 */
public class TrackerRevisionMismatchException extends BusinessRuleException {

    private final TrackerId trackerId;
    private final long currentRevision;
    private final long expectedRevision;

    /**
     * Constructs a TrackerRevisionMismatchException with tracker details and revisions.
     *
     * @param trackerId        the identifier of the tracker
     * @param currentRevision  the actual current aggregate revision
     * @param expectedRevision the revision expected by the client in If-Match
     */
    public TrackerRevisionMismatchException(TrackerId trackerId, long currentRevision, long expectedRevision) {
        super("phenology.tracker.revision.mismatch");
        this.trackerId = trackerId;
        this.currentRevision = currentRevision;
        this.expectedRevision = expectedRevision;
    }

    public TrackerId getTrackerId() {
        return trackerId;
    }

    public long getCurrentRevision() {
        return currentRevision;
    }

    public long getExpectedRevision() {
        return expectedRevision;
    }
}

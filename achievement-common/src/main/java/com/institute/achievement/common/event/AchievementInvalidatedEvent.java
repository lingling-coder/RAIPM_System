package com.institute.achievement.common.event;

import lombok.Getter;

/**
 * Event published when an achievement is invalidated (e.g. patent revoked).
 * <p>
 * Listened to by {@code PatentInvalidationListener} in the fee module to
 * auto-pause all pending/paused fee plans and fee records associated with
 * the invalidated achievement.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // publisher (invalidation service):
 * eventPublisher.publishEvent(new AchievementInvalidatedEvent(this, "patent", patentId, "专利失效"));
 * }</pre>
 */
@Getter
public class AchievementInvalidatedEvent {

    /** Source object that published the event */
    private final Object source;

    /** Achievement type — "patent" or "copyright" */
    private final String ownerType;

    /** Achievement entity ID */
    private final Long ownerId;

    /** Reason for invalidation */
    private final String reason;

    /**
     * Create a new achievement invalidation event.
     *
     * @param source    the object that published the event
     * @param ownerType achievement type ("patent" or "copyright")
     * @param ownerId   achievement entity ID
     * @param reason    invalidation reason
     */
    public AchievementInvalidatedEvent(Object source, String ownerType, Long ownerId, String reason) {
        this.source = source;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.reason = reason;
    }
}

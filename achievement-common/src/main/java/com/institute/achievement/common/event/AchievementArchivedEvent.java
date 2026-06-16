package com.institute.achievement.common.event;

import lombok.Getter;

import java.time.LocalDate;

/**
 * Event published when an achievement transitions to ARCHIVED status.
 * <p>
 * Listened to by {@code FirstFeeGenerationListener} in the fee module to
 * auto-generate the first fee record from the achievement's nextFeeDate.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // publisher (patent service):
 * eventPublisher.publishEvent(new AchievementArchivedEvent(this, "patent", patentId, "ARCHIVED", patent.getNextFeeDate()));
 * }</pre>
 */
@Getter
public class AchievementArchivedEvent {

    /** Source object that published the event */
    private final Object source;

    /** Achievement type — "patent" or "copyright" */
    private final String ownerType;

    /** Achievement entity ID */
    private final Long ownerId;

    /** Achievement status after the transition */
    private final String achievementStatus;

    /** Next fee due date (for auto-generating first fee record) */
    private final LocalDate nextFeeDate;

    /**
     * Create a new achievement archived event.
     *
     * @param source            the object that published the event
     * @param ownerType         achievement type ("patent" or "copyright")
     * @param ownerId           achievement entity ID
     * @param achievementStatus the target status ("ARCHIVED")
     * @param nextFeeDate       the achievement's next fee due date (nullable)
     */
    public AchievementArchivedEvent(Object source, String ownerType, Long ownerId,
                                    String achievementStatus, LocalDate nextFeeDate) {
        this.source = source;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.achievementStatus = achievementStatus;
        this.nextFeeDate = nextFeeDate;
    }
}

package dev.lizarazu.profileapi.profile;

import java.util.List;

public record BusinessProfileResponse(
        String uei,
        boolean sampleData,
        RegistrationSection registration,
        AwardHistorySection awardHistory,
        List<String> notices
) {

    public enum SourceStatus {
        SAMPLE,
        AVAILABLE,
        NO_RECORDS,
        UNAVAILABLE
    }

    public record RegistrationSection(
            SourceStatus status,
            String legalBusinessName,
            String registrationStatus
    ) {
    }

    public record AwardHistorySection(
            SourceStatus status,
            List<AwardSummary> awards
    ) {
    }

    public record AwardSummary(
            String awardId,
            String description,
            String awardingAgency
    ) {
    }
}
package dev.lizarazu.profileapi.usaspending;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UsaSpendingAward(
        @JsonProperty("Award ID") String awardId,
        @JsonProperty("Recipient Name") String recipientName,
        @JsonProperty("Recipient UEI") String recipientUei,
        @JsonProperty("Award Amount") BigDecimal awardAmount,
        @JsonProperty("Awarding Agency") String awardingAgency,
        @JsonProperty("Description") String description,
        @JsonProperty("Start Date") LocalDate startDate,
        @JsonProperty("End Date") LocalDate endDate,
        @JsonProperty("generated_internal_id") String generatedInternalId
) {
}
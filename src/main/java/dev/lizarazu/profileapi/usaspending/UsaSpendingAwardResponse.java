package dev.lizarazu.profileapi.usaspending;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UsaSpendingAwardResponse(
        List<UsaSpendingAward> results,
        @JsonProperty("page_metadata") UsaSpendingPageMetadata pageMetadata
) {
}
package dev.lizarazu.profileapi.usaspending;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UsaSpendingAwardSearchRequest(
        Filters filters,
        List<String> fields,
        int limit,
        int page,
        String sort,
        String order
) {

    public record Filters(
            @JsonProperty("recipient_search_text") List<String> recipientSearchText,
            @JsonProperty("award_type_codes") List<String> awardTypeCodes,
            @JsonProperty("time_period") List<TimePeriod> timePeriod
    ) {
    }

    public record TimePeriod(
            @JsonProperty("start_date") String startDate,
            @JsonProperty("end_date") String endDate
    ) {
    }
}
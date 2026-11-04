package dev.lizarazu.profileapi.usaspending;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class UsaSpendingClient {

    static final List<String> CONTRACT_AWARD_TYPES = List.of("A", "B", "C", "D");

    static final List<String> FIELDS = List.of(
            "Award ID", "Recipient Name", "Recipient UEI", "Award Amount",
            "Awarding Agency", "Description", "Start Date", "End Date",
            "generated_internal_id"
    );

    private final RestClient restClient;

    public UsaSpendingClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public UsaSpendingAwardResponse searchContracts(
            String recipientName,
            LocalDate startDate,
            LocalDate endDate,
            int limit
    ) {
        UsaSpendingAwardSearchRequest request = new UsaSpendingAwardSearchRequest(
                new UsaSpendingAwardSearchRequest.Filters(
                        List.of(recipientName),
                        CONTRACT_AWARD_TYPES,
                        List.of(new UsaSpendingAwardSearchRequest.TimePeriod(
                                startDate.toString(),
                                endDate.toString()
                        ))
                ),
                FIELDS,
                limit,
                1,
                "Award Amount",
                "desc"
        );

        return restClient.post()
                .uri("/api/v2/search/spending_by_award/")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(UsaSpendingAwardResponse.class);
    }
}
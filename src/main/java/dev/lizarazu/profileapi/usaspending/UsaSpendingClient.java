package dev.lizarazu.profileapi.usaspending;

import java.time.Clock;
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
    private final Clock clock;
    private final int lookbackYears;
    private final int resultLimit;

    public UsaSpendingClient(
            RestClient restClient,
            Clock clock,
            int lookbackYears,
            int resultLimit
    ) {
        this.restClient = restClient;
        this.clock = clock;
        this.lookbackYears = lookbackYears;
        this.resultLimit = resultLimit;
    }

    public UsaSpendingAwardResponse searchRecentContracts(String recipientName) {
        LocalDate endDate = LocalDate.now(clock);
        return searchContracts(
                recipientName,
                endDate.minusYears(lookbackYears),
                endDate,
                resultLimit
        );
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
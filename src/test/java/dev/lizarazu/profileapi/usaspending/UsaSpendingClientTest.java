package dev.lizarazu.profileapi.usaspending;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class UsaSpendingClientTest {

    private static final String URL =
            "https://usaspending.example/api/v2/search/spending_by_award/";

    private MockRestServiceServer server;
    private UsaSpendingClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://usaspending.example");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new UsaSpendingClient(builder.build());
    }

    @Test
    void searchesContractsByRecipientNameAndMapsResponse() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.filters.recipient_search_text[0]")
                        .value("BULLS RUN GROUP, LLC, THE"))
                .andExpect(jsonPath("$.filters.award_type_codes.length()").value(4))
                .andExpect(jsonPath("$.filters.time_period[0].start_date")
                        .value("2021-10-01"))
                .andExpect(jsonPath("$.filters.time_period[0].end_date")
                        .value("2026-09-30"))
                .andExpect(jsonPath("$.fields[2]").value("Recipient UEI"))
                .andExpect(jsonPath("$.limit").value(5))
                .andRespond(withSuccess(
                        """
                        {
                          "spending_level": "awards",
                          "limit": 5,
                          "results": [
                            {
                              "internal_id": 351894090,
                              "Award ID": "FA238425CB024",
                              "Recipient Name": "BULLS RUN GROUP, LLC, THE",
                              "Recipient UEI": "KDH9SC281WL8",
                              "Award Amount": 1022727.0,
                              "Awarding Agency": "Department of Defense",
                              "Description": "DELIVER A COGNITIVE PSYCHOLOGY-BASED IW SUPPORT TOOL.",
                              "Start Date": "2025-08-11",
                              "End Date": "2027-05-11",
                              "generated_internal_id": "CONT_AWD_FA238425CB024_9700_-NONE-_-NONE-",
                              "awarding_agency_id": 1173,
                              "agency_slug": "department-of-defense"
                            }
                          ],
                          "page_metadata": {
                            "page": 1,
                            "hasNext": false,
                            "last_record_unique_id": null,
                            "last_record_sort_value": "None"
                          },
                          "messages": ["Informational message"]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        UsaSpendingAwardResponse response = client.searchContracts(
                "BULLS RUN GROUP, LLC, THE",
                LocalDate.of(2021, 10, 1),
                LocalDate.of(2026, 9, 30),
                5
        );

        server.verify();
        assertEquals(1, response.results().size());
        UsaSpendingAward award = response.results().getFirst();
        assertEquals("FA238425CB024", award.awardId());
        assertEquals("KDH9SC281WL8", award.recipientUei());
        assertEquals(0, new BigDecimal("1022727.0").compareTo(award.awardAmount()));
        assertEquals("Department of Defense", award.awardingAgency());
        assertEquals(LocalDate.of(2025, 8, 11), award.startDate());
        assertEquals(LocalDate.of(2027, 5, 11), award.endDate());
        assertFalse(response.pageMetadata().hasNext());
    }

    @Test
    void mapsEmptyResults() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess(
                        """
                        {
                          "results": [],
                          "page_metadata": { "page": 1, "hasNext": false }
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        UsaSpendingAwardResponse response = client.searchContracts(
                "NO SUCH BUSINESS", LocalDate.of(2021, 10, 1),
                LocalDate.of(2026, 9, 30), 5
        );

        assertTrue(response.results().isEmpty());
    }

    @Test
    void throwsOnBadGateway() {
        server.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThrows(RestClientException.class, () -> client.searchContracts(
                "BULLS RUN GROUP, LLC, THE", LocalDate.of(2021, 10, 1),
                LocalDate.of(2026, 9, 30), 5
        ));
    }
}
package dev.lizarazu.profileapi.sam;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class SamEntityClientTest {

    @Test
    void requestsRegistrationByUeiAndMapsResponse() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://sam.example");

        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        SamEntityClient client = new SamEntityClient(
                builder.build(),
                "test-api-key"
        );

        server.expect(requestTo(
                        "https://sam.example/entity-information/v3/entities"
                                + "?api_key=test-api-key"
                                + "&ueiSAM=ABCD12345678"
                                + "&includeSections=entityRegistration"
                ))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Accept", "application/json"))
                .andRespond(withSuccess(
                        """
                        {
                          "totalRecords": 1,
                          "entityData": [
                            {
                              "entityRegistration": {
                                "ueiSAM": "ABCD12345678",
                                "legalBusinessName": "Example Test Business",
                                "registrationStatus": "Active"
                              }
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        SamEntityResponse response = client.findByUei("ABCD12345678");

        assertNotNull(response);
        assertEquals(1, response.totalRecords());
        assertNotNull(response.entityData());
        assertEquals(1, response.entityData().size());

        SamEntityRegistration registration =
                response.entityData().getFirst().entityRegistration();

        assertNotNull(registration);
        assertEquals("ABCD12345678", registration.ueiSAM());
        assertEquals(
                "Example Test Business",
                registration.legalBusinessName()
        );
        assertEquals("Active", registration.registrationStatus());

        server.verify();
    }

    @Test
void mapsResponseWithNoMatchingEntities() {
    RestClient.Builder builder = RestClient.builder()
            .baseUrl("https://sam.example");

    MockRestServiceServer server =
            MockRestServiceServer.bindTo(builder).build();

    SamEntityClient client = new SamEntityClient(
            builder.build(),
            "test-api-key"
    );

    server.expect(requestTo(
                    "https://sam.example/entity-information/v3/entities"
                            + "?api_key=test-api-key"
                            + "&ueiSAM=ABCD12345678"
                            + "&includeSections=entityRegistration"
            ))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(
                    """
                    {
                      "totalRecords": 0,
                      "entityData": []
                    }
                    """,
                    MediaType.APPLICATION_JSON
            ));

    SamEntityResponse response = client.findByUei("ABCD12345678");

    assertNotNull(response);
    assertEquals(0, response.totalRecords());
    assertNotNull(response.entityData());
    assertTrue(response.entityData().isEmpty());

    server.verify();
}

    @Test
void propagatesUpstreamServiceFailure() {
    RestClient.Builder builder = RestClient.builder()
            .baseUrl("https://sam.example");

    MockRestServiceServer server =
            MockRestServiceServer.bindTo(builder).build();

    SamEntityClient client = new SamEntityClient(
            builder.build(),
            "test-api-key"
    );

    server.expect(requestTo(
                    "https://sam.example/entity-information/v3/entities"
                            + "?api_key=test-api-key"
                            + "&ueiSAM=ABCD12345678"
                            + "&includeSections=entityRegistration"
            ))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

    RestClientResponseException exception = assertThrows(
            RestClientResponseException.class,
            () -> client.findByUei("ABCD12345678")
    );

    assertEquals(
            HttpStatus.SERVICE_UNAVAILABLE,
            exception.getStatusCode()
    );

    server.verify();
}
}

package dev.lizarazu.profileapi.profile;

import java.util.List;
import java.util.Optional;

import dev.lizarazu.profileapi.sam.SamEntityClient;
import dev.lizarazu.profileapi.sam.SamEntityResponse;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BusinessProfileServiceTest {

    private final BusinessProfileService service =
            new BusinessProfileService(Optional.empty());

    @Test
    void createsClearlyLabeledSampleProfile() {
        BusinessProfileResponse response =
                service.getProfile("DEMO12345678");

        assertEquals("DEMO12345678", response.uei());
        assertTrue(response.sampleData());

        assertEquals(
                BusinessProfileResponse.SourceStatus.SAMPLE,
                response.registration().status()
        );

        assertEquals(1, response.awardHistory().awards().size());

        assertTrue(response.notices().contains(
                "Demonstration only. No government sources were queried."
        ));
    }

    @Test
    void usesTheIdentifierProvidedToTheService() {
        BusinessProfileResponse response =
                service.getProfile("ANOTHER12345");

        assertEquals("ANOTHER12345", response.uei());
    }

    @Test
    void reportsUnavailableWhenSamLookupFails() {
        SamEntityClient client = mock(SamEntityClient.class);
        when(client.findByUei("DEMO12345678"))
                .thenThrow(new RestClientException("upstream failure"));

        BusinessProfileResponse response =
                new BusinessProfileService(Optional.of(client))
                        .getProfile("DEMO12345678");

        assertEquals(
                BusinessProfileResponse.SourceStatus.UNAVAILABLE,
                response.registration().status()
        );
        assertNull(response.registration().legalBusinessName());
        assertTrue(response.notices().stream()
                .noneMatch(notice -> notice.contains("upstream failure")));
    }

    @Test
    void reportsUnavailableWhenSamReturnsNoBody() {
        SamEntityClient client = mock(SamEntityClient.class);
        when(client.findByUei("DEMO12345678")).thenReturn(null);

        BusinessProfileResponse response =
                new BusinessProfileService(Optional.of(client))
                        .getProfile("DEMO12345678");

        assertEquals(
                BusinessProfileResponse.SourceStatus.UNAVAILABLE,
                response.registration().status()
        );
    }

    @Test
    void reportsNoRecordsWhenSamFindsNothing() {
        SamEntityClient client = mock(SamEntityClient.class);
        SamEntityResponse samResponse = mock(SamEntityResponse.class);
        when(samResponse.totalRecords()).thenReturn(0);
        when(samResponse.entityData()).thenReturn(List.of());
        when(client.findByUei("DEMO12345678")).thenReturn(samResponse);

        BusinessProfileResponse response =
                new BusinessProfileService(Optional.of(client))
                        .getProfile("DEMO12345678");

        assertEquals(
                BusinessProfileResponse.SourceStatus.NO_RECORDS,
                response.registration().status()
        );
        assertTrue(response.sampleData());
        assertEquals(
                BusinessProfileResponse.SourceStatus.SAMPLE,
                response.awardHistory().status()
        );
    }
}
package dev.lizarazu.profileapi.profile;

import java.util.List;
import java.util.Optional;

import dev.lizarazu.profileapi.sam.SamEntityClient;
import dev.lizarazu.profileapi.sam.SamEntityResponse;
import dev.lizarazu.profileapi.sam.SamEntity;
import dev.lizarazu.profileapi.sam.SamEntityRegistration;

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

        private static final String UEI = "DEMO12345678";

    private BusinessProfileResponse profileWith(SamEntityResponse samResponse) {
        SamEntityClient client = mock(SamEntityClient.class);
        when(client.findByUei(UEI)).thenReturn(samResponse);
        return new BusinessProfileService(Optional.of(client)).getProfile(UEI);
    }

    private SamEntity entity(String uei, String name, String status) {
        return new SamEntity(new SamEntityRegistration(uei, name, status));
    }

    @Test
    void reportsAvailableForSingleMatchingRegistration() {
        BusinessProfileResponse response = profileWith(new SamEntityResponse(
                1, List.of(entity(UEI, "Acme Federal LLC", "Active"))
        ));

        assertEquals(
                BusinessProfileResponse.SourceStatus.AVAILABLE,
                response.registration().status()
        );
        assertEquals("Acme Federal LLC", response.registration().legalBusinessName());
        assertEquals(
                BusinessProfileResponse.SourceStatus.SAMPLE,
                response.awardHistory().status()
        );
    }

    @Test
    void reportsUnavailableWhenUeiDoesNotMatch() {
        BusinessProfileResponse response = profileWith(new SamEntityResponse(
                1, List.of(entity("OTHER1234567", "Acme Federal LLC", "Active"))
        ));

        assertEquals(
                BusinessProfileResponse.SourceStatus.UNAVAILABLE,
                response.registration().status()
        );
        assertNull(response.registration().legalBusinessName());
    }

    @Test
    void reportsUnavailableWhenBusinessNameIsBlank() {
        BusinessProfileResponse response = profileWith(new SamEntityResponse(
                1, List.of(entity(UEI, "  ", "Active"))
        ));

        assertEquals(
                BusinessProfileResponse.SourceStatus.UNAVAILABLE,
                response.registration().status()
        );
    }

    @Test
    void reportsUnavailableWhenMultipleRecordsReturned() {
        BusinessProfileResponse response = profileWith(new SamEntityResponse(
                2, List.of(
                        entity(UEI, "Acme Federal LLC", "Active"),
                        entity(UEI, "Acme Duplicate LLC", "Active")
                )
        ));

        assertEquals(
                BusinessProfileResponse.SourceStatus.UNAVAILABLE,
                response.registration().status()
        );
    }

    @Test
    void reportsUnavailableWhenCountAndDataDisagree() {
        BusinessProfileResponse response =
                profileWith(new SamEntityResponse(1, List.of()));

        assertEquals(
                BusinessProfileResponse.SourceStatus.UNAVAILABLE,
                response.registration().status()
        );
    }

    @Test
    void reportsUnavailableWhenRegistrationSectionMissing() {
        BusinessProfileResponse response = profileWith(new SamEntityResponse(
                1, List.of(new SamEntity(null))
        ));

        assertEquals(
                BusinessProfileResponse.SourceStatus.UNAVAILABLE,
                response.registration().status()
        );
    }
}
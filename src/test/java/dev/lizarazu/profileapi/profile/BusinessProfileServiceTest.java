package dev.lizarazu.profileapi.profile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import dev.lizarazu.profileapi.sam.SamEntity;
import dev.lizarazu.profileapi.sam.SamEntityClient;
import dev.lizarazu.profileapi.sam.SamEntityRegistration;
import dev.lizarazu.profileapi.sam.SamEntityResponse;
import dev.lizarazu.profileapi.usaspending.UsaSpendingAward;
import dev.lizarazu.profileapi.usaspending.UsaSpendingAwardResponse;
import dev.lizarazu.profileapi.usaspending.UsaSpendingClient;
import dev.lizarazu.profileapi.usaspending.UsaSpendingPageMetadata;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BusinessProfileServiceTest {

    private static final String UEI = "DEMO12345678";

    private final BusinessProfileService service =
            new BusinessProfileService(Optional.empty(), Optional.empty());

    // ---- Helpers ----

    private BusinessProfileResponse profileWith(SamEntityResponse samResponse) {
        SamEntityClient client = mock(SamEntityClient.class);
        when(client.findByUei(UEI)).thenReturn(samResponse);
        return new BusinessProfileService(Optional.of(client), Optional.empty())
                .getProfile(UEI);
    }

    private SamEntity entity(String uei, String name, String status) {
        return new SamEntity(new SamEntityRegistration(uei, name, status));
    }

    private SamEntityClient availableSamClient() {
        SamEntityClient client = mock(SamEntityClient.class);
        when(client.findByUei(UEI)).thenReturn(new SamEntityResponse(
                1, List.of(entity(UEI, "Acme Federal LLC", "Active"))
        ));
        return client;
    }

    private UsaSpendingAward award(String awardId, String recipientUei) {
        return new UsaSpendingAward(
                awardId,
                "Acme Federal LLC",
                recipientUei,
                new BigDecimal("1000.0"),
                "Department of Defense",
                "Test award",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 1, 1),
                "CONT_AWD_" + awardId
        );
    }

    private UsaSpendingAwardResponse awardResponse(UsaSpendingAward... awards) {
        return new UsaSpendingAwardResponse(
                List.of(awards),
                new UsaSpendingPageMetadata(1, false)
        );
    }

    // ---- Sample mode ----

    @Test
    void createsClearlyLabeledSampleProfile() {
        BusinessProfileResponse response = service.getProfile(UEI);

        assertEquals(UEI, response.uei());
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
        BusinessProfileResponse response = service.getProfile("ANOTHER12345");

        assertEquals("ANOTHER12345", response.uei());
    }

    // ---- Registration (SAM.gov) ----

    @Test
    void reportsUnavailableWhenSamLookupFails() {
        SamEntityClient client = mock(SamEntityClient.class);
        when(client.findByUei(UEI))
                .thenThrow(new RestClientException("upstream failure"));

        BusinessProfileResponse response =
                new BusinessProfileService(Optional.of(client), Optional.empty())
                        .getProfile(UEI);

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
        BusinessProfileResponse response = profileWith(null);

        assertEquals(
                BusinessProfileResponse.SourceStatus.UNAVAILABLE,
                response.registration().status()
        );
    }

    @Test
    void reportsNoRecordsWhenSamFindsNothing() {
        BusinessProfileResponse response =
                profileWith(new SamEntityResponse(0, List.of()));

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

    // ---- Award history (USAspending.gov) ----

    @Test
    void reportsAvailableAwardsMatchingTheUei() {
        UsaSpendingClient usaClient = mock(UsaSpendingClient.class);
        when(usaClient.searchRecentContracts("Acme Federal LLC")).thenReturn(
                awardResponse(award("AWARD-1", UEI), award("AWARD-2", "OTHER1234567"))
        );

        BusinessProfileResponse response = new BusinessProfileService(
                Optional.of(availableSamClient()), Optional.of(usaClient)
        ).getProfile(UEI);

        assertEquals(
                BusinessProfileResponse.SourceStatus.AVAILABLE,
                response.awardHistory().status()
        );
        assertEquals(1, response.awardHistory().awards().size());
        assertEquals("AWARD-1", response.awardHistory().awards().getFirst().awardId());
        assertFalse(response.sampleData());
        assertTrue(response.notices().contains(
                "Award history retrieved from USAspending.gov prime contract awards."
        ));
    }

    @Test
    void reportsNoRecordsWhenNoAwardMatchesTheUei() {
        UsaSpendingClient usaClient = mock(UsaSpendingClient.class);
        when(usaClient.searchRecentContracts("Acme Federal LLC"))
                .thenReturn(awardResponse(award("AWARD-2", "OTHER1234567")));

        BusinessProfileResponse response = new BusinessProfileService(
                Optional.of(availableSamClient()), Optional.of(usaClient)
        ).getProfile(UEI);

        assertEquals(
                BusinessProfileResponse.SourceStatus.NO_RECORDS,
                response.awardHistory().status()
        );
        assertTrue(response.awardHistory().awards().isEmpty());
    }

    @Test
    void reportsNoRecordsWhenUsaSpendingReturnsEmptyResults() {
        UsaSpendingClient usaClient = mock(UsaSpendingClient.class);
        when(usaClient.searchRecentContracts("Acme Federal LLC"))
                .thenReturn(awardResponse());

        BusinessProfileResponse response = new BusinessProfileService(
                Optional.of(availableSamClient()), Optional.of(usaClient)
        ).getProfile(UEI);

        assertEquals(
                BusinessProfileResponse.SourceStatus.NO_RECORDS,
                response.awardHistory().status()
        );
    }

    @Test
    void reportsUnavailableAwardsWhenUsaSpendingFails() {
        UsaSpendingClient usaClient = mock(UsaSpendingClient.class);
        when(usaClient.searchRecentContracts(anyString()))
                .thenThrow(new RestClientException("502 Bad Gateway secret-detail"));

        BusinessProfileResponse response = new BusinessProfileService(
                Optional.of(availableSamClient()), Optional.of(usaClient)
        ).getProfile(UEI);

        assertEquals(
                BusinessProfileResponse.SourceStatus.UNAVAILABLE,
                response.awardHistory().status()
        );
        assertTrue(response.awardHistory().awards().isEmpty());
        assertEquals(
                BusinessProfileResponse.SourceStatus.AVAILABLE,
                response.registration().status()
        );
        assertTrue(response.notices().stream()
                .noneMatch(notice -> notice.contains("secret-detail")));
    }

    @Test
    void reportsUnavailableAwardsWhenUsaSpendingReturnsNoBody() {
        UsaSpendingClient usaClient = mock(UsaSpendingClient.class);
        when(usaClient.searchRecentContracts(anyString())).thenReturn(null);

        BusinessProfileResponse response = new BusinessProfileService(
                Optional.of(availableSamClient()), Optional.of(usaClient)
        ).getProfile(UEI);

        assertEquals(
                BusinessProfileResponse.SourceStatus.UNAVAILABLE,
                response.awardHistory().status()
        );
    }

    @Test
    void skipsAwardSearchWhenRegistrationIsNotAvailable() {
        SamEntityClient samClient = mock(SamEntityClient.class);
        when(samClient.findByUei(UEI)).thenReturn(new SamEntityResponse(0, List.of()));
        UsaSpendingClient usaClient = mock(UsaSpendingClient.class);

        BusinessProfileResponse response = new BusinessProfileService(
                Optional.of(samClient), Optional.of(usaClient)
        ).getProfile(UEI);

        assertEquals(
                BusinessProfileResponse.SourceStatus.UNAVAILABLE,
                response.awardHistory().status()
        );
        verify(usaClient, never()).searchRecentContracts(anyString());
    }
}
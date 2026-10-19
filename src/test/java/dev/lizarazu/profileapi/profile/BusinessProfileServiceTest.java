package dev.lizarazu.profileapi.profile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BusinessProfileServiceTest {

    private final BusinessProfileService service =
            new BusinessProfileService();

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
}

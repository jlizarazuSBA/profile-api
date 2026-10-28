package dev.lizarazu.profileapi.profile;

import java.util.List;

import dev.lizarazu.profileapi.sam.SamEntity;
import dev.lizarazu.profileapi.sam.SamEntityClient;
import dev.lizarazu.profileapi.sam.SamEntityRegistration;
import dev.lizarazu.profileapi.sam.SamEntityResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BusinessProfileController.class)
@Import({
        BusinessProfileService.class,
        BusinessProfileExceptionHandler.class
})
class BusinessProfileControllerSamTest {

    private static final String UEI = "ABCD12345678";
    private static final String PATH = "/api/v1/business-profiles/" + UEI;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SamEntityClient samClient;

    @Test
    void returnsAvailableRegistrationFromSam() throws Exception {
        when(samClient.findByUei(UEI)).thenReturn(new SamEntityResponse(
                1,
                List.of(new SamEntity(new SamEntityRegistration(
                        UEI, "Acme Federal LLC", "Active"
                )))
        ));

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.uei").value(UEI))
                .andExpect(jsonPath("$.sampleData").value(true))
                .andExpect(jsonPath("$.registration.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.registration.legalBusinessName")
                        .value("Acme Federal LLC"))
                .andExpect(jsonPath("$.registration.registrationStatus")
                        .value("Active"))
                .andExpect(jsonPath("$.awardHistory.status").value("SAMPLE"))
                .andExpect(jsonPath("$.notices[0]").value(
                        "SAM.gov registration lookup was attempted for the supplied UEI."
                ));
    }

    @Test
    void returnsNoRecordsWhenSamFindsNothing() throws Exception {
        when(samClient.findByUei(UEI))
                .thenReturn(new SamEntityResponse(0, List.of()));

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registration.status").value("NO_RECORDS"))
                .andExpect(jsonPath("$.registration.legalBusinessName")
                        .value(nullValue()))
                .andExpect(jsonPath("$.awardHistory.status").value("SAMPLE"));
    }

    @Test
    void returnsUnavailableWithoutLeakingUpstreamError() throws Exception {
        when(samClient.findByUei(UEI)).thenThrow(
                new RestClientException("upstream failure: secret-detail")
        );

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registration.status").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.registration.legalBusinessName")
                        .value(nullValue()))
                .andExpect(jsonPath("$.sampleData").value(true))
                .andExpect(content().string(not(containsString("secret-detail"))))
                .andExpect(content().string(not(containsString("upstream failure"))));
    }
}
package dev.lizarazu.profileapi.profile;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BusinessProfileController.class)
@Import(BusinessProfileService.class)
class BusinessProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsClearlyLabeledSampleProfile() throws Exception {
        mockMvc.perform(
                        get("/api/v1/business-profiles/DEMO12345678")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.uei").value("DEMO12345678"))
                .andExpect(jsonPath("$.sampleData").value(true))
                .andExpect(jsonPath("$.registration.status").value("SAMPLE"))
                .andExpect(jsonPath("$.awardHistory.status").value("SAMPLE"))
                .andExpect(jsonPath("$.awardHistory.awards").isArray())
                .andExpect(jsonPath("$.notices[0]").value(
                        "Demonstration only. No government sources were queried."
                ));
    }

    @Test
    void echoesTheIdentifierFromTheRequestPath() throws Exception {
        mockMvc.perform(
                        get("/api/v1/business-profiles/ANOTHER12345")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uei").value("ANOTHER12345"));
    }
}
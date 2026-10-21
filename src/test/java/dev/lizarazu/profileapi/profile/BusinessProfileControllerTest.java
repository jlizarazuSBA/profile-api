package dev.lizarazu.profileapi.profile;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BusinessProfileController.class)
@Import({
        BusinessProfileService.class,
        BusinessProfileExceptionHandler.class
})


class BusinessProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsClearlyLabeledSampleProfile() throws Exception {
        mockMvc.perform(
                        get("/api/v1/business-profiles/ABCD12345678")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.uei").value("ABCD12345678"))
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
                        get("/api/v1/business-profiles/WXYZ12345678")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uei").value("WXYZ12345678"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ABCD1234567",
            "ABCD123456789",
            "ABCD1234567-",
            "ABCD1234567 ",
            "ABCI12345678",
            "ABCO12345678",
            "0BCD12345678",
            "abcd12345678"
    })
    void rejectsMalformedUeiWithClearError(String uei) throws Exception {
        mockMvc.perform(
                        get("/api/v1/business-profiles/{uei}", uei)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value(
                        "Request validation failed"
                ))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(
                        "UEI must contain 12 uppercase letters or digits, "
                                + "must not contain I or O, and must not start with 0"
                ));
    }
}
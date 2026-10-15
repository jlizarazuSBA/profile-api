package dev.lizarazu.profileapi.profile;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/business-profiles")
public class BusinessProfileController {

    @GetMapping("/{uei}")
    public BusinessProfileResponse getProfile(
            @PathVariable("uei") String uei
    ) {
        return new BusinessProfileResponse(
                uei,
                true,
                new BusinessProfileResponse.RegistrationSection(
                        BusinessProfileResponse.SourceStatus.SAMPLE,
                        "Example Business — Sample Data",
                        "SAMPLE_ONLY"
                ),
                new BusinessProfileResponse.AwardHistorySection(
                        BusinessProfileResponse.SourceStatus.SAMPLE,
                        List.of(
                                new BusinessProfileResponse.AwardSummary(
                                        "SAMPLE-AWARD-001",
                                        "Fictional award for endpoint demonstration",
                                        "Example Agency — Sample Data"
                                )
                        )
                ),
                List.of(
                        "Demonstration only. No government sources were queried.",
                        "The supplied UEI is echoed without validation or lookup."
                )
        );
    }
}
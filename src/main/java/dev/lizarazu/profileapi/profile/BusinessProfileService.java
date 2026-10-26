package dev.lizarazu.profileapi.profile;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class BusinessProfileService {

    public BusinessProfileResponse getProfile(String uei) {
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
                        "The supplied UEI is format-validated only; its existence in SAM.gov has not been verified."
                )
        );
    }
}
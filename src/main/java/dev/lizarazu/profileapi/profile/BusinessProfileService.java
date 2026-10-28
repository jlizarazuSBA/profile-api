package dev.lizarazu.profileapi.profile;

import java.util.List;
import java.util.Optional;

import dev.lizarazu.profileapi.sam.SamEntityClient;
import dev.lizarazu.profileapi.sam.SamEntityRegistration;
import dev.lizarazu.profileapi.sam.SamEntityResponse;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class BusinessProfileService {

    private final Optional<SamEntityClient> samClient;

    public BusinessProfileService(Optional<SamEntityClient> samClient) {
        this.samClient = samClient;
    }

    public BusinessProfileResponse getProfile(String uei) {
        BusinessProfileResponse.RegistrationSection registration;
        List<String> notices;

        if (samClient.isEmpty()) {
            registration = new BusinessProfileResponse.RegistrationSection(
                    BusinessProfileResponse.SourceStatus.SAMPLE,
                    "Example Business — Sample Data",
                    "SAMPLE_ONLY"
            );

            notices = List.of(
                    "Demonstration only. No government sources were queried.",
                    "The supplied UEI is format-validated only; "
                            + "its existence in SAM.gov has not been verified."
            );
        } else {
            registration = lookupRegistration(uei);

            notices = List.of(
                    "SAM.gov registration lookup was attempted for the supplied UEI.",
                    "Award history contains fictional sample data; "
                            + "no award source was queried."
            );
        }

        return new BusinessProfileResponse(
                uei,
                true,
                registration,
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
                notices
        );
    }

    private BusinessProfileResponse.RegistrationSection lookupRegistration(
            String uei
    ) {
        SamEntityResponse response;

        try {
            response = samClient.orElseThrow().findByUei(uei);
        } catch (RestClientException exception) {
            return unavailableRegistration();
        }

        if (response == null || response.entityData() == null) {
            return unavailableRegistration();
        }

        if (response.totalRecords() == 0
                && response.entityData().isEmpty()) {
            return new BusinessProfileResponse.RegistrationSection(
                    BusinessProfileResponse.SourceStatus.NO_RECORDS,
                    null,
                    null
            );
        }

        if (response.totalRecords() != 1
                || response.entityData().size() != 1
                || response.entityData().getFirst() == null) {
            return unavailableRegistration();
        }

        SamEntityRegistration registration =
                response.entityData().getFirst().entityRegistration();

        if (registration == null
                || !uei.equals(registration.ueiSAM())
                || registration.legalBusinessName() == null
                || registration.legalBusinessName().isBlank()
                || registration.registrationStatus() == null
                || registration.registrationStatus().isBlank()) {
            return unavailableRegistration();
        }

        return new BusinessProfileResponse.RegistrationSection(
                BusinessProfileResponse.SourceStatus.AVAILABLE,
                registration.legalBusinessName(),
                registration.registrationStatus()
        );
    }

    private BusinessProfileResponse.RegistrationSection unavailableRegistration() {
        return new BusinessProfileResponse.RegistrationSection(
                BusinessProfileResponse.SourceStatus.UNAVAILABLE,
                null,
                null
        );
    }
}
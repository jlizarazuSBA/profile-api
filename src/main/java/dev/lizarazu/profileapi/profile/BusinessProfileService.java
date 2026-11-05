package dev.lizarazu.profileapi.profile;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import dev.lizarazu.profileapi.profile.BusinessProfileResponse.AwardHistorySection;
import dev.lizarazu.profileapi.profile.BusinessProfileResponse.AwardSummary;
import dev.lizarazu.profileapi.profile.BusinessProfileResponse.RegistrationSection;
import dev.lizarazu.profileapi.profile.BusinessProfileResponse.SourceStatus;
import dev.lizarazu.profileapi.sam.SamEntityClient;
import dev.lizarazu.profileapi.sam.SamEntityRegistration;
import dev.lizarazu.profileapi.sam.SamEntityResponse;
import dev.lizarazu.profileapi.usaspending.UsaSpendingAwardResponse;
import dev.lizarazu.profileapi.usaspending.UsaSpendingClient;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class BusinessProfileService {

    private final Optional<SamEntityClient> samClient;
    private final Optional<UsaSpendingClient> usaSpendingClient;

    public BusinessProfileService(
            Optional<SamEntityClient> samClient,
            Optional<UsaSpendingClient> usaSpendingClient
    ) {
        this.samClient = samClient;
        this.usaSpendingClient = usaSpendingClient;
    }

    public BusinessProfileResponse getProfile(String uei) {
        RegistrationSection registration = samClient.isPresent()
                ? lookupRegistration(uei)
                : sampleRegistration();

        AwardHistorySection awardHistory = usaSpendingClient.isPresent()
                ? lookupAwards(uei, registration)
                : sampleAwards();

        boolean sampleData = registration.status() == SourceStatus.SAMPLE
                || awardHistory.status() == SourceStatus.SAMPLE;

        return new BusinessProfileResponse(
                uei,
                sampleData,
                registration,
                awardHistory,
                notices(registration.status(), awardHistory.status())
        );
    }

    // ---- Registration (SAM.gov) ----

    private RegistrationSection lookupRegistration(String uei) {
        SamEntityResponse response;

        try {
            response = samClient.orElseThrow().findByUei(uei);
        } catch (RestClientException exception) {
            return unavailableRegistration();
        }

        if (response == null || response.entityData() == null) {
            return unavailableRegistration();
        }

        if (response.totalRecords() == 0 && response.entityData().isEmpty()) {
            return new RegistrationSection(SourceStatus.NO_RECORDS, null, null);
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
                || isBlank(registration.legalBusinessName())
                || isBlank(registration.registrationStatus())) {
            return unavailableRegistration();
        }

        return new RegistrationSection(
                SourceStatus.AVAILABLE,
                registration.legalBusinessName(),
                registration.registrationStatus()
        );
    }

    private RegistrationSection sampleRegistration() {
        return new RegistrationSection(
                SourceStatus.SAMPLE,
                "Example Business — Sample Data",
                "SAMPLE_ONLY"
        );
    }

    private RegistrationSection unavailableRegistration() {
        return new RegistrationSection(SourceStatus.UNAVAILABLE, null, null);
    }

    // ---- Award history (USAspending.gov) ----

    private AwardHistorySection lookupAwards(
            String uei,
            RegistrationSection registration
    ) {
        if (registration.status() != SourceStatus.AVAILABLE) {
            return unavailableAwards();
        }

        UsaSpendingAwardResponse response;

        try {
            response = usaSpendingClient.orElseThrow()
                    .searchRecentContracts(registration.legalBusinessName());
        } catch (RestClientException exception) {
            return unavailableAwards();
        }

        if (response == null || response.results() == null) {
            return unavailableAwards();
        }

        List<AwardSummary> awards = response.results().stream()
                .filter(Objects::nonNull)
                .filter(award -> uei.equals(award.recipientUei()))
                .filter(award -> !isBlank(award.awardId()))
                .map(award -> new AwardSummary(
                        award.awardId(),
                        award.description(),
                        award.awardingAgency()
                ))
                .toList();

        if (awards.isEmpty()) {
            return new AwardHistorySection(SourceStatus.NO_RECORDS, List.of());
        }

        return new AwardHistorySection(SourceStatus.AVAILABLE, awards);
    }

    private AwardHistorySection sampleAwards() {
        return new AwardHistorySection(
                SourceStatus.SAMPLE,
                List.of(new AwardSummary(
                        "SAMPLE-AWARD-001",
                        "Fictional award for endpoint demonstration",
                        "Example Agency — Sample Data"
                ))
        );
    }

    private AwardHistorySection unavailableAwards() {
        return new AwardHistorySection(SourceStatus.UNAVAILABLE, List.of());
    }

    // ---- Notices ----

    private List<String> notices(SourceStatus registration, SourceStatus awards) {
        if (registration == SourceStatus.SAMPLE && awards == SourceStatus.SAMPLE) {
            return List.of(
                    "Demonstration only. No government sources were queried.",
                    "The supplied UEI is format-validated only; "
                            + "its existence in SAM.gov has not been verified."
            );
        }

        return List.of(registrationNotice(registration), awardNotice(awards));
    }

    private String registrationNotice(SourceStatus status) {
        return switch (status) {
            case SAMPLE -> "Registration contains fictional sample data; "
                    + "SAM.gov was not queried.";
            case AVAILABLE -> "Registration data retrieved from SAM.gov.";
            case NO_RECORDS -> "SAM.gov returned no public registration "
                    + "for the supplied UEI.";
            case UNAVAILABLE -> "SAM.gov registration data is currently unavailable.";
        };
    }

    private String awardNotice(SourceStatus status) {
        return switch (status) {
            case SAMPLE -> "Award history contains fictional sample data; "
                    + "no award source was queried.";
            case AVAILABLE -> "Award history retrieved from USAspending.gov "
                    + "prime contract awards.";
            case NO_RECORDS -> "USAspending.gov returned no prime contract awards "
                    + "for the supplied UEI.";
            case UNAVAILABLE -> "USAspending.gov award history is currently unavailable.";
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
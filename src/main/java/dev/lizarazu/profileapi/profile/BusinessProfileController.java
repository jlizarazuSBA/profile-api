package dev.lizarazu.profileapi.profile;

import jakarta.validation.constraints.Pattern;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/business-profiles")
public class BusinessProfileController {

    private final BusinessProfileService profileService;

    public BusinessProfileController(
            BusinessProfileService profileService
    ) {
        this.profileService = profileService;
    }

    @GetMapping("/{uei}")
    public BusinessProfileResponse getProfile(
            @PathVariable("uei")
            @Pattern(
                    regexp = "[1-9A-HJ-NP-Z][0-9A-HJ-NP-Z]{11}",
                    message = "UEI must contain 12 uppercase letters or digits, "
                            + "must not contain I or O, and must not start with 0"
            )
            String uei
    ) {
        return profileService.getProfile(uei);
    }
}
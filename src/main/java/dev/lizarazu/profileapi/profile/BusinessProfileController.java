package dev.lizarazu.profileapi.profile;

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
            @PathVariable("uei") String uei
    ) {
        return profileService.getProfile(uei);
    }
}
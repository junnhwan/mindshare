package com.mindshare.profile.api;

import com.mindshare.auth.token.JwtService;
import com.mindshare.profile.api.dto.ProfilePatchRequest;
import com.mindshare.profile.api.dto.ProfileResponse;
import com.mindshare.profile.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@Validated
public class ProfileController {

    private final ProfileService profileService;
    private final JwtService jwtService;

    public ProfileController(ProfileService profileService, JwtService jwtService) {
        this.profileService = profileService;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ProfileResponse current(@AuthenticationPrincipal Jwt jwt) {
        return profileService.getCurrentProfile(jwtService.extractUserId(jwt));
    }

    @PatchMapping
    public ProfileResponse patch(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfilePatchRequest request) {
        return profileService.updateProfile(jwtService.extractUserId(jwt), request);
    }
}

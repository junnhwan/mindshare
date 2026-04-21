package com.mindshare.profile.api;

import com.mindshare.auth.token.JwtService;
import com.mindshare.profile.api.dto.ProfilePatchRequest;
import com.mindshare.profile.api.dto.ProfileResponse;
import com.mindshare.profile.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mindshare.storage.OssStorageService;

@RestController
@RequestMapping("/api/v1/profile")
@Validated
@Profile("!bootstrap-test")
public class ProfileController {

    private final ProfileService profileService;
    private final JwtService jwtService;
    private final OssStorageService ossStorageService;

    public ProfileController(ProfileService profileService, JwtService jwtService, OssStorageService ossStorageService) {
        this.profileService = profileService;
        this.jwtService = jwtService;
        this.ossStorageService = ossStorageService;
    }

    @GetMapping
    public ProfileResponse current(@AuthenticationPrincipal Jwt jwt) {
        return profileService.getCurrentProfile(jwtService.extractUserId(jwt));
    }

    @PatchMapping
    public ProfileResponse patch(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfilePatchRequest request) {
        return profileService.updateProfile(jwtService.extractUserId(jwt), request);
    }

    @PostMapping("/avatar")
    public ProfileResponse uploadAvatar(@AuthenticationPrincipal Jwt jwt, @RequestPart("file") MultipartFile file) {
        long userId = jwtService.extractUserId(jwt);
        String avatarUrl = ossStorageService.uploadAvatar(userId, file);
        return profileService.updateAvatar(userId, avatarUrl);
    }
}

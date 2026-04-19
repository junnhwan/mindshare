package com.mindshare.profile.service;

import com.mindshare.profile.api.dto.ProfilePatchRequest;
import com.mindshare.profile.api.dto.ProfileResponse;

public interface ProfileService {

    ProfileResponse getCurrentProfile(long userId);

    ProfileResponse updateProfile(long userId, ProfilePatchRequest request);
}

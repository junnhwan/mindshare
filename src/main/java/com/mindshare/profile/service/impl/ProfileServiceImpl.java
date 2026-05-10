package com.mindshare.profile.service.impl;

import com.mindshare.common.exception.BusinessException;
import com.mindshare.common.exception.ErrorCode;
import com.mindshare.profile.api.dto.ProfilePatchRequest;
import com.mindshare.profile.api.dto.ProfileResponse;
import com.mindshare.profile.service.ProfileService;
import com.mindshare.user.domain.User;
import com.mindshare.user.mapper.UserMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!bootstrap-test")
public class ProfileServiceImpl implements ProfileService {

    private final UserMapper userMapper;

    public ProfileServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getCurrentProfile(long userId) {
        return map(loadUser(userId));
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(long userId, ProfilePatchRequest request) {
        User patch = new User();
        patch.setId(userId);
        patch.setNickname(request.nickname());
        patch.setAvatar(request.avatar());
        patch.setBio(request.bio());
        patch.setGender(request.gender());
        patch.setBirthday(request.birthday());
        patch.setSchool(request.school());
        patch.setTagsJson(request.tagJson());
        userMapper.updateProfile(patch);
        return map(loadUser(userId));
    }

    @Override
    @Transactional
    public ProfileResponse updateAvatar(long userId, String avatarUrl) {
        loadUser(userId);
        User patch = new User();
        patch.setId(userId);
        patch.setAvatar(avatarUrl);
        userMapper.updateProfile(patch);
        return map(loadUser(userId));
    }

    private User loadUser(long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND, "user not found");
        }
        return user;
    }

    private ProfileResponse map(User user) {
        return new ProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getAvatar(),
                user.getBio(),
                user.getGender(),
                user.getBirthday(),
                user.getSchool(),
                user.getPhone(),
                user.getEmail(),
                user.getTagsJson()
        );
    }
}

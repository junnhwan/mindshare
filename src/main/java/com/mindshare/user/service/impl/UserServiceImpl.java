package com.mindshare.user.service.impl;

import com.mindshare.user.domain.User;
import com.mindshare.user.mapper.UserMapper;
import com.mindshare.user.service.UserService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@Profile("!bootstrap-test")
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByPhone(String phone) {
        return Optional.ofNullable(userMapper.findByPhone(phone));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(userMapper.findByEmail(email));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(long id) {
        return Optional.ofNullable(userMapper.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByPhone(String phone) {
        return userMapper.existsByPhone(phone);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userMapper.existsByEmail(email);
    }

    @Override
    @Transactional
    public User createUser(User user) {
        Instant now = Instant.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        userMapper.insert(user);
        return user;
    }

    @Override
    @Transactional
    public void updatePassword(User user) {
        user.setUpdateTime(Instant.now());
        userMapper.updatePassword(user.getId(), user.getPasswordHash());
    }
}

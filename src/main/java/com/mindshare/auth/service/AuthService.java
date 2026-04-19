package com.mindshare.auth.service;

import com.mindshare.auth.api.dto.AuthResponse;
import com.mindshare.auth.api.dto.AuthUserResponse;
import com.mindshare.auth.api.dto.LoginRequest;
import com.mindshare.auth.api.dto.PasswordResetRequest;
import com.mindshare.auth.api.dto.RegisterRequest;
import com.mindshare.auth.api.dto.SendCodeRequest;
import com.mindshare.auth.api.dto.SendCodeResponse;
import com.mindshare.auth.api.dto.TokenRefreshRequest;
import com.mindshare.auth.api.dto.TokenResponse;
import com.mindshare.auth.audit.LoginLog;
import com.mindshare.auth.audit.LoginLogMapper;
import com.mindshare.auth.model.ClientInfo;
import com.mindshare.auth.model.IdentifierType;
import com.mindshare.auth.token.JwtService;
import com.mindshare.auth.token.RefreshTokenStore;
import com.mindshare.auth.token.TokenPair;
import com.mindshare.auth.verification.SendCodeResult;
import com.mindshare.auth.verification.VerificationCheckResult;
import com.mindshare.auth.verification.VerificationCodeStatus;
import com.mindshare.auth.verification.VerificationScene;
import com.mindshare.auth.verification.VerificationService;
import com.mindshare.common.exception.BusinessException;
import com.mindshare.common.exception.ErrorCode;
import com.mindshare.user.domain.User;
import com.mindshare.user.service.UserService;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
@Profile("!bootstrap-test")
public class AuthService {

    private final UserService userService;
    private final VerificationService verificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;
    private final LoginLogMapper loginLogMapper;

    public AuthService(
            UserService userService,
            VerificationService verificationService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenStore refreshTokenStore,
            LoginLogMapper loginLogMapper
    ) {
        this.userService = userService;
        this.verificationService = verificationService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenStore = refreshTokenStore;
        this.loginLogMapper = loginLogMapper;
    }

    @Transactional
    public SendCodeResponse sendCode(SendCodeRequest request) {
        validateIdentifier(request.identifierType(), request.identifier());
        String identifier = normalizeIdentifier(request.identifierType(), request.identifier());
        boolean exists = identifierExists(request.identifierType(), identifier);
        if (request.scene() == VerificationScene.REGISTER && exists) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "identifier already exists");
        }
        if ((request.scene() == VerificationScene.LOGIN || request.scene() == VerificationScene.RESET_PASSWORD) && !exists) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "identifier not found");
        }

        SendCodeResult result = verificationService.sendCode(request.scene(), identifier);
        return new SendCodeResponse(result.identifier(), result.scene(), result.expireSeconds());
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, ClientInfo clientInfo) {
        validateIdentifier(request.identifierType(), request.identifier());
        validatePassword(request.password());

        String identifier = normalizeIdentifier(request.identifierType(), request.identifier());
        if (identifierExists(request.identifierType(), identifier)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "identifier already exists");
        }

        ensureVerificationSuccess(verificationService.verify(VerificationScene.REGISTER, identifier, request.code()));

        User user = new User();
        if (request.identifierType() == IdentifierType.PHONE) {
            user.setPhone(identifier);
        } else {
            user.setEmail(identifier);
        }
        user.setPasswordHash(passwordEncoder.encode(request.password().trim()));
        user.setNickname(StringUtils.hasText(request.nickname()) ? request.nickname().trim() : defaultNickname(identifier));
        user.setTagsJson("[]");

        userService.createUser(user);
        TokenPair tokenPair = jwtService.issueTokenPair(user);
        storeRefreshToken(user.getId(), tokenPair);
        recordLoginLog(user.getId(), identifier, "REGISTER", clientInfo, "SUCCESS");

        return new AuthResponse(mapUser(user), mapToken(tokenPair));
    }

    @Transactional
    public AuthResponse login(LoginRequest request, ClientInfo clientInfo) {
        validateIdentifier(request.identifierType(), request.identifier());
        String identifier = normalizeIdentifier(request.identifierType(), request.identifier());
        User user = findUserByIdentifier(request.identifierType(), identifier)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "identifier not found"));

        String channel;
        if (StringUtils.hasText(request.password())) {
            channel = "PASSWORD";
            if (!StringUtils.hasText(user.getPasswordHash()) || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                recordLoginLog(user.getId(), identifier, channel, clientInfo, "FAILED");
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "invalid credentials");
            }
        } else if (StringUtils.hasText(request.code())) {
            channel = "CODE";
            ensureVerificationSuccess(verificationService.verify(VerificationScene.LOGIN, identifier, request.code()));
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "password or code is required");
        }

        TokenPair tokenPair = jwtService.issueTokenPair(user);
        storeRefreshToken(user.getId(), tokenPair);
        recordLoginLog(user.getId(), identifier, channel, clientInfo, "SUCCESS");
        return new AuthResponse(mapUser(user), mapToken(tokenPair));
    }

    @Transactional
    public TokenResponse refresh(TokenRefreshRequest request) {
        Jwt jwt = decodeRefreshToken(request.refreshToken());
        if (!Objects.equals("refresh", jwtService.extractTokenType(jwt))) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "invalid refresh token");
        }

        long userId = jwtService.extractUserId(jwt);
        String tokenId = jwtService.extractTokenId(jwt);
        if (!refreshTokenStore.isTokenValid(userId, tokenId)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "invalid refresh token");
        }

        User user = userService.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "user not found"));
        TokenPair tokenPair = jwtService.issueTokenPair(user);
        refreshTokenStore.revokeToken(userId, tokenId);
        storeRefreshToken(userId, tokenPair);
        return mapToken(tokenPair);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }
        try {
            Jwt jwt = jwtService.decode(refreshToken);
            if (Objects.equals("refresh", jwtService.extractTokenType(jwt))) {
                refreshTokenStore.revokeToken(jwtService.extractUserId(jwt), jwtService.extractTokenId(jwt));
            }
        } catch (JwtException ignored) {
            // Ignore invalid refresh token on logout.
        }
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        validateIdentifier(request.identifierType(), request.identifier());
        validatePassword(request.newPassword());

        String identifier = normalizeIdentifier(request.identifierType(), request.identifier());
        User user = findUserByIdentifier(request.identifierType(), identifier)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "user not found"));

        ensureVerificationSuccess(verificationService.verify(VerificationScene.RESET_PASSWORD, identifier, request.code()));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword().trim()));
        userService.updatePassword(user);
        refreshTokenStore.revokeAll(user.getId());
    }

    @Transactional(readOnly = true)
    public AuthUserResponse me(long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "user not found"));
        return mapUser(user);
    }

    private void validateIdentifier(IdentifierType type, String identifier) {
        if (!StringUtils.hasText(identifier)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "identifier is required");
        }
        String value = identifier.trim();
        switch (type) {
            case PHONE -> {
                if (!value.matches("^1\\d{10}$")) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "invalid phone");
                }
            }
            case EMAIL -> {
                if (!value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "invalid email");
                }
            }
        }
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password) || password.trim().length() < 8) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "password must be at least 8 chars");
        }
        String text = password.trim();
        boolean hasLetter = text.chars().anyMatch(Character::isLetter);
        boolean hasDigit = text.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "password must include letters and digits");
        }
    }

    private String normalizeIdentifier(IdentifierType type, String identifier) {
        return switch (type) {
            case PHONE -> identifier.trim();
            case EMAIL -> identifier.trim().toLowerCase(Locale.ROOT);
        };
    }

    private boolean identifierExists(IdentifierType type, String identifier) {
        return switch (type) {
            case PHONE -> userService.existsByPhone(identifier);
            case EMAIL -> userService.existsByEmail(identifier);
        };
    }

    private Optional<User> findUserByIdentifier(IdentifierType type, String identifier) {
        return switch (type) {
            case PHONE -> userService.findByPhone(identifier);
            case EMAIL -> userService.findByEmail(identifier);
        };
    }

    private void ensureVerificationSuccess(VerificationCheckResult result) {
        if (result.success()) {
            return;
        }
        if (result.status() == VerificationCodeStatus.NOT_FOUND) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED, "verification code expired");
        }
        if (result.status() == VerificationCodeStatus.TOO_MANY_ATTEMPTS) {
            throw new BusinessException(ErrorCode.TOO_MANY_ATTEMPTS, "too many verification attempts");
        }
        throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID, "verification code invalid");
    }

    private Jwt decodeRefreshToken(String refreshToken) {
        try {
            return jwtService.decode(refreshToken);
        } catch (JwtException exception) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "invalid refresh token");
        }
    }

    private void storeRefreshToken(Long userId, TokenPair tokenPair) {
        Duration ttl = Duration.between(Instant.now(), tokenPair.refreshTokenExpiresAt());
        if (ttl.isNegative()) {
            ttl = Duration.ZERO;
        }
        refreshTokenStore.storeToken(userId, tokenPair.refreshTokenId(), ttl);
    }

    private void recordLoginLog(Long userId, String identifier, String channel, ClientInfo clientInfo, String result) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setIdentifier(identifier);
        log.setChannel(channel);
        log.setIp(clientInfo.ip());
        log.setUserAgent(clientInfo.userAgent());
        log.setResult(result);
        log.setCreateTime(Instant.now());
        loginLogMapper.insert(log);
    }

    private AuthUserResponse mapUser(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getNickname(),
                user.getAvatar(),
                user.getPhone(),
                user.getEmail()
        );
    }

    private TokenResponse mapToken(TokenPair tokenPair) {
        return new TokenResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.tokenType(),
                tokenPair.expiresIn()
        );
    }

    private String defaultNickname(String identifier) {
        String value = identifier.contains("@") ? identifier.substring(0, identifier.indexOf('@')) : identifier;
        return "ms_" + value;
    }
}

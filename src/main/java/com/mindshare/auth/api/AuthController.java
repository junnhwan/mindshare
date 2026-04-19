package com.mindshare.auth.api;

import com.mindshare.auth.api.dto.AuthResponse;
import com.mindshare.auth.api.dto.AuthUserResponse;
import com.mindshare.auth.api.dto.LoginRequest;
import com.mindshare.auth.api.dto.LogoutRequest;
import com.mindshare.auth.api.dto.PasswordResetRequest;
import com.mindshare.auth.api.dto.RegisterRequest;
import com.mindshare.auth.api.dto.SendCodeRequest;
import com.mindshare.auth.api.dto.SendCodeResponse;
import com.mindshare.auth.api.dto.TokenRefreshRequest;
import com.mindshare.auth.api.dto.TokenResponse;
import com.mindshare.auth.model.ClientInfo;
import com.mindshare.auth.service.AuthService;
import com.mindshare.auth.token.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
@Profile("!bootstrap-test")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/send-code")
    public SendCodeResponse sendCode(@Valid @RequestBody SendCodeRequest request) {
        return authService.sendCode(request);
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpServletRequest) {
        return authService.register(request, resolveClient(httpServletRequest));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        return authService.login(request, resolveClient(httpServletRequest));
    }

    @PostMapping("/token/refresh")
    public TokenResponse refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public AuthUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(jwtService.extractUserId(jwt));
    }

    private ClientInfo resolveClient(HttpServletRequest request) {
        return new ClientInfo(extractClientIp(request), request.getHeader("User-Agent"));
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}

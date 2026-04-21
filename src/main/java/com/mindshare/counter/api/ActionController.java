package com.mindshare.counter.api;

import com.mindshare.auth.token.JwtService;
import com.mindshare.counter.api.dto.ActionRequest;
import com.mindshare.counter.service.CounterService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/action")
@Profile("!bootstrap-test")
public class ActionController {

    private final CounterService counterService;
    private final JwtService jwtService;

    public ActionController(CounterService counterService, JwtService jwtService) {
        this.counterService = counterService;
        this.jwtService = jwtService;
    }

    @PostMapping("/like")
    public ResponseEntity<Map<String, Object>> like(
            @Valid @RequestBody ActionRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        long userId = jwtService.extractUserId(jwt);
        boolean changed = counterService.like(request.entityType(), request.entityId(), userId);
        return ResponseEntity.ok(Map.of(
                "changed", changed,
                "liked", counterService.isLiked(request.entityType(), request.entityId(), userId)
        ));
    }

    @PostMapping("/unlike")
    public ResponseEntity<Map<String, Object>> unlike(
            @Valid @RequestBody ActionRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        long userId = jwtService.extractUserId(jwt);
        boolean changed = counterService.unlike(request.entityType(), request.entityId(), userId);
        return ResponseEntity.ok(Map.of(
                "changed", changed,
                "liked", counterService.isLiked(request.entityType(), request.entityId(), userId)
        ));
    }

    @PostMapping("/fav")
    public ResponseEntity<Map<String, Object>> fav(
            @Valid @RequestBody ActionRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        long userId = jwtService.extractUserId(jwt);
        boolean changed = counterService.fav(request.entityType(), request.entityId(), userId);
        return ResponseEntity.ok(Map.of(
                "changed", changed,
                "faved", counterService.isFaved(request.entityType(), request.entityId(), userId)
        ));
    }

    @PostMapping("/unfav")
    public ResponseEntity<Map<String, Object>> unfav(
            @Valid @RequestBody ActionRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        long userId = jwtService.extractUserId(jwt);
        boolean changed = counterService.unfav(request.entityType(), request.entityId(), userId);
        return ResponseEntity.ok(Map.of(
                "changed", changed,
                "faved", counterService.isFaved(request.entityType(), request.entityId(), userId)
        ));
    }
}

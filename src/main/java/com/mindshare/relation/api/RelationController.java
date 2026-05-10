package com.mindshare.relation.api;

import com.mindshare.auth.token.JwtService;
import com.mindshare.relation.service.RelationService;
import com.mindshare.user.domain.User;
import com.mindshare.user.mapper.UserMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/relation")
@Profile("!bootstrap-test")
public class RelationController {

    private final RelationService relationService;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public RelationController(RelationService relationService, JwtService jwtService, UserMapper userMapper) {
        this.relationService = relationService;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @PostMapping("/follow")
    public Map<String, Object> follow(@AuthenticationPrincipal Jwt jwt,
                                       @RequestParam("toUserId") long toUserId) {
        long fromUserId = jwtService.extractUserId(jwt);
        boolean changed = relationService.follow(fromUserId, toUserId);
        return Map.of("changed", changed, "status", relationService.relationStatus(fromUserId, toUserId));
    }

    @PostMapping("/unfollow")
    public Map<String, Object> unfollow(@AuthenticationPrincipal Jwt jwt,
                                         @RequestParam("toUserId") long toUserId) {
        long fromUserId = jwtService.extractUserId(jwt);
        boolean changed = relationService.unfollow(fromUserId, toUserId);
        return Map.of("changed", changed, "status", relationService.relationStatus(fromUserId, toUserId));
    }

    @GetMapping("/status")
    public Map<String, Object> status(@AuthenticationPrincipal Jwt jwt,
                                       @RequestParam("userId") long otherUserId) {
        long fromUserId = jwtService.extractUserId(jwt);
        return Map.of("status", relationService.relationStatus(fromUserId, otherUserId));
    }

    @GetMapping("/following")
    public Map<String, Object> following(@RequestParam(value = "userId", required = false) Long userId,
                                          @RequestParam(value = "page", defaultValue = "1") int page,
                                          @RequestParam(value = "size", defaultValue = "20") int size,
                                          @AuthenticationPrincipal Jwt jwt) {
        long targetUserId = userId != null ? userId : jwtService.extractUserId(jwt);
        List<Long> ids = relationService.listFollowingIds(targetUserId, page, size);
        List<Map<String, Object>> profiles = userMapper.listByIds(ids).stream()
                .map(this::userToProfile)
                .collect(Collectors.toList());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", profiles);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @GetMapping("/followers")
    public Map<String, Object> followers(@RequestParam(value = "userId", required = false) Long userId,
                                          @RequestParam(value = "page", defaultValue = "1") int page,
                                          @RequestParam(value = "size", defaultValue = "20") int size,
                                          @AuthenticationPrincipal Jwt jwt) {
        long targetUserId = userId != null ? userId : jwtService.extractUserId(jwt);
        List<Long> ids = relationService.listFollowerIds(targetUserId, page, size);
        List<Map<String, Object>> profiles = userMapper.listByIds(ids).stream()
                .map(this::userToProfile)
                .collect(Collectors.toList());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", profiles);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @GetMapping("/counter")
    public Map<String, Object> counter(@RequestParam("userId") long userId) {
        return Map.of(
                "following", relationService.countFollowing(userId),
                "followers", relationService.countFollowers(userId)
        );
    }

    private Map<String, Object> userToProfile(User user) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("id", user.getId());
        p.put("nickname", user.getNickname());
        p.put("avatar", user.getAvatar());
        p.put("bio", user.getBio());
        p.put("zgId", user.getZgId());
        return p;
    }
}

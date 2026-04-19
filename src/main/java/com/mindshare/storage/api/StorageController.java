package com.mindshare.storage.api;

import com.mindshare.auth.token.JwtService;
import com.mindshare.common.exception.BusinessException;
import com.mindshare.common.exception.ErrorCode;
import com.mindshare.knowpost.mapper.KnowPostMapper;
import com.mindshare.knowpost.model.KnowPost;
import com.mindshare.storage.OssStorageService;
import com.mindshare.storage.api.dto.StoragePresignRequest;
import com.mindshare.storage.api.dto.StoragePresignResponse;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/storage")
@Validated
@Profile("!bootstrap-test")
public class StorageController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneOffset.UTC);

    private final OssStorageService ossStorageService;
    private final JwtService jwtService;
    private final KnowPostMapper knowPostMapper;

    public StorageController(
            OssStorageService ossStorageService,
            JwtService jwtService,
            KnowPostMapper knowPostMapper
    ) {
        this.ossStorageService = ossStorageService;
        this.jwtService = jwtService;
        this.knowPostMapper = knowPostMapper;
    }

    @PostMapping("/presign")
    public StoragePresignResponse presign(
            @Valid @RequestBody StoragePresignRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        long userId = jwtService.extractUserId(jwt);
        long postId = parsePostId(request.postId());
        validateOwnership(userId, postId);

        String ext = normalizeExt(request.ext(), request.contentType(), request.scene());
        String objectKey = buildObjectKey(postId, request.scene(), ext);
        int expiresIn = ossStorageService.getPresignExpireSeconds();
        String putUrl = ossStorageService.generatePresignedPutUrl(objectKey, request.contentType(), expiresIn);
        return new StoragePresignResponse(
                objectKey,
                putUrl,
                Map.of("Content-Type", request.contentType()),
                expiresIn
        );
    }

    private long parsePostId(String postIdText) {
        try {
            return Long.parseLong(postIdText);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "invalid postId");
        }
    }

    private void validateOwnership(long userId, long postId) {
        KnowPost post = knowPostMapper.findById(postId);
        if (post == null || post.getCreatorId() == null || post.getCreatorId() != userId) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "draft not found or no permission");
        }
    }

    private String buildObjectKey(long postId, String scene, String ext) {
        if ("knowpost_content".equals(scene)) {
            return "posts/" + postId + "/content" + ext;
        }
        if ("knowpost_image".equals(scene)) {
            String date = DATE_FORMATTER.format(Instant.now());
            String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            return "posts/" + postId + "/images/" + date + "/" + random + ext;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "unsupported upload scene");
    }

    private String normalizeExt(String ext, String contentType, String scene) {
        if (ext != null && !ext.isBlank()) {
            return ext.startsWith(".") ? ext : "." + ext;
        }
        if ("knowpost_content".equals(scene)) {
            return switch (contentType) {
                case "text/markdown" -> ".md";
                case "text/html" -> ".html";
                case "text/plain" -> ".txt";
                case "application/json" -> ".json";
                default -> ".bin";
            };
        }
        if ("knowpost_image".equals(scene)) {
            return switch (contentType) {
                case "image/jpeg" -> ".jpg";
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                case "image/gif" -> ".gif";
                default -> ".img";
            };
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "unsupported upload scene");
    }
}

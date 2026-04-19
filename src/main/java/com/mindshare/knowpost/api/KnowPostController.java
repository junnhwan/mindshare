package com.mindshare.knowpost.api;

import com.mindshare.auth.token.JwtService;
import com.mindshare.knowpost.api.dto.DescriptionSuggestRequest;
import com.mindshare.knowpost.api.dto.DescriptionSuggestResponse;
import com.mindshare.knowpost.api.dto.FeedPageResponse;
import com.mindshare.knowpost.api.dto.KnowPostContentConfirmRequest;
import com.mindshare.knowpost.api.dto.KnowPostDetailResponse;
import com.mindshare.knowpost.api.dto.KnowPostDraftCreateResponse;
import com.mindshare.knowpost.api.dto.KnowPostPatchRequest;
import com.mindshare.knowpost.api.dto.KnowPostTopPatchRequest;
import com.mindshare.knowpost.api.dto.KnowPostVisibilityPatchRequest;
import com.mindshare.knowpost.service.KnowPostFeedService;
import com.mindshare.knowpost.service.KnowPostService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/knowposts")
@Validated
@Profile("!bootstrap-test")
public class KnowPostController {

    private final KnowPostService knowPostService;
    private final KnowPostFeedService knowPostFeedService;
    private final JwtService jwtService;

    public KnowPostController(KnowPostService knowPostService, KnowPostFeedService knowPostFeedService, JwtService jwtService) {
        this.knowPostService = knowPostService;
        this.knowPostFeedService = knowPostFeedService;
        this.jwtService = jwtService;
    }

    @PostMapping("/drafts")
    public KnowPostDraftCreateResponse createDraft(@AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        return new KnowPostDraftCreateResponse(String.valueOf(knowPostService.createDraft(userId)));
    }

    @PostMapping("/{id}/content/confirm")
    public ResponseEntity<Void> confirmContent(@PathVariable("id") long id,
                                               @Valid @RequestBody KnowPostContentConfirmRequest request,
                                               @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        knowPostService.confirmContent(userId, id, request.objectKey(), request.etag(), request.size(), request.sha256());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> patchMetadata(@PathVariable("id") long id,
                                              @Valid @RequestBody KnowPostPatchRequest request,
                                              @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        knowPostService.updateMetadata(
                userId,
                id,
                request.title(),
                request.tagId(),
                request.tags(),
                request.imgUrls(),
                request.visible(),
                request.isTop(),
                request.description()
        );
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/top")
    public ResponseEntity<Void> patchTop(@PathVariable("id") long id,
                                         @Valid @RequestBody KnowPostTopPatchRequest request,
                                         @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        knowPostService.updateTop(userId, id, request.isTop());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<Void> patchVisibility(@PathVariable("id") long id,
                                                @Valid @RequestBody KnowPostVisibilityPatchRequest request,
                                                @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        knowPostService.updateVisibility(userId, id, request.visible());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") long id,
                                       @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        knowPostService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<Void> publish(@PathVariable("id") long id,
                                        @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        knowPostService.publish(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/feed")
    public FeedPageResponse feed(@RequestParam(value = "page", defaultValue = "1") int page,
                                 @RequestParam(value = "size", defaultValue = "20") int size,
                                 @AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt == null ? null : jwtService.extractUserId(jwt);
        return knowPostFeedService.getPublicFeed(page, size, userId);
    }

    @GetMapping("/mine")
    public FeedPageResponse mine(@RequestParam(value = "page", defaultValue = "1") int page,
                                 @RequestParam(value = "size", defaultValue = "20") int size,
                                 @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        return knowPostFeedService.getMyPublished(userId, page, size);
    }

    @GetMapping("/detail/{id}")
    public KnowPostDetailResponse detail(@PathVariable("id") long id,
                                         @AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt == null ? null : jwtService.extractUserId(jwt);
        return knowPostService.getDetail(id, userId);
    }

    @PostMapping("/description/suggest")
    public DescriptionSuggestResponse suggestDescription(@Valid @RequestBody DescriptionSuggestRequest request) {
        return knowPostService.suggestDescription(request.content());
    }
}

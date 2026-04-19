package com.mindshare.search.api;

import com.mindshare.auth.token.JwtService;
import com.mindshare.search.api.dto.SearchResponse;
import com.mindshare.search.api.dto.SuggestResponse;
import com.mindshare.search.service.SearchService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@Validated
@Profile("!bootstrap-test")
public class SearchController {

    private final SearchService searchService;
    private final JwtService jwtService;

    public SearchController(SearchService searchService, JwtService jwtService) {
        this.searchService = searchService;
        this.jwtService = jwtService;
    }

    @GetMapping
    public SearchResponse search(@RequestParam("q") @NotBlank String q,
                                 @RequestParam(value = "size", defaultValue = "20") @Min(1) int size,
                                 @RequestParam(value = "tags", required = false) String tagsCsv,
                                 @RequestParam(value = "after", required = false) String after,
                                 @AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt == null ? null : jwtService.extractUserId(jwt);
        return searchService.search(q, size, tagsCsv, after, userId);
    }

    @GetMapping("/suggest")
    public SuggestResponse suggest(@RequestParam("prefix") @NotBlank String prefix,
                                   @RequestParam(value = "size", defaultValue = "10") @Min(1) int size) {
        return searchService.suggest(prefix, size);
    }
}

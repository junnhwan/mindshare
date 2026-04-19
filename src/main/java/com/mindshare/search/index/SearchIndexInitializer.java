package com.mindshare.search.index;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!bootstrap-test")
public class SearchIndexInitializer {

    private final SearchIndexService searchIndexService;

    public SearchIndexInitializer(SearchIndexService searchIndexService) {
        this.searchIndexService = searchIndexService;
    }

    @PostConstruct
    public void initialize() {
        searchIndexService.initialize();
    }
}

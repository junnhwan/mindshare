package com.mindshare.search.service.impl;

import com.mindshare.search.api.dto.SearchResponse;
import com.mindshare.search.api.dto.SuggestResponse;
import com.mindshare.search.index.SearchIndexService;
import com.mindshare.search.service.SearchService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!bootstrap-test")
public class SearchServiceImpl implements SearchService {

    private final SearchIndexService searchIndexService;

    public SearchServiceImpl(SearchIndexService searchIndexService) {
        this.searchIndexService = searchIndexService;
    }

    @Override
    public SearchResponse search(String q, int size, String tagsCsv, String after, Long currentUserIdNullable) {
        return searchIndexService.search(q, size, tagsCsv, after, currentUserIdNullable);
    }

    @Override
    public SuggestResponse suggest(String prefix, int size) {
        return searchIndexService.suggest(prefix, size);
    }
}

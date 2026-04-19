package com.mindshare.search.service;

import com.mindshare.search.api.dto.SearchResponse;
import com.mindshare.search.api.dto.SuggestResponse;

public interface SearchService {

    SearchResponse search(String q, int size, String tagsCsv, String after, Long currentUserIdNullable);

    SuggestResponse suggest(String prefix, int size);
}

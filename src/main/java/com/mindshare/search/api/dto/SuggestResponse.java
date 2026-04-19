package com.mindshare.search.api.dto;

import java.util.List;

public record SuggestResponse(
        List<String> items
) {
}

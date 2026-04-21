package com.mindshare.search.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindshare.config.ElasticsearchConfig;
import com.mindshare.knowpost.api.dto.FeedItemResponse;
import com.mindshare.knowpost.mapper.KnowPostMapper;
import com.mindshare.knowpost.model.KnowPostDetailRow;
import com.mindshare.knowpost.model.KnowPostFeedRow;
import com.mindshare.knowpost.service.KnowPostContentLoader;
import com.mindshare.search.api.dto.SearchResponse;
import com.mindshare.search.api.dto.SuggestResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
@Profile("!bootstrap-test")
public class SearchIndexService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_BODY_LENGTH = 4000;
    private static final int BODY_SNIPPET_RADIUS = 36;

    private final ObjectProvider<ElasticsearchClient> elasticsearchClientProvider;
    private final ElasticsearchConfig.MindShareElasticsearchProperties elasticsearchProperties;
    private final KnowPostMapper knowPostMapper;
    private final ObjectMapper objectMapper;
    private final KnowPostContentLoader knowPostContentLoader;
    private final ConcurrentMap<Long, SearchDocument> inMemoryDocuments = new ConcurrentHashMap<>();

    public SearchIndexService(
            ObjectProvider<ElasticsearchClient> elasticsearchClientProvider,
            ElasticsearchConfig.MindShareElasticsearchProperties elasticsearchProperties,
            KnowPostMapper knowPostMapper,
            ObjectMapper objectMapper,
            KnowPostContentLoader knowPostContentLoader
    ) {
        this.elasticsearchClientProvider = elasticsearchClientProvider;
        this.elasticsearchProperties = elasticsearchProperties;
        this.knowPostMapper = knowPostMapper;
        this.objectMapper = objectMapper;
        this.knowPostContentLoader = knowPostContentLoader;
    }

    public void initialize() {
        try {
            ensureIndex();
            rebuildFromDatabase();
        } catch (Exception ignored) {
        }
    }

    public void upsertKnowPost(long id) {
        SearchDocument document = buildDocument(id);
        if (document == null) {
            deleteKnowPost(id);
            return;
        }
        inMemoryDocuments.put(id, document);

        ElasticsearchClient client = elasticsearchClient();
        if (client == null) {
            return;
        }
        try {
            client.index(request -> request
                    .index(indexName())
                    .id(String.valueOf(id))
                    .document(document.toMap())
                    .refresh(Refresh.WaitFor));
        } catch (Exception ignored) {
        }
    }

    public void deleteKnowPost(long id) {
        inMemoryDocuments.remove(id);

        ElasticsearchClient client = elasticsearchClient();
        if (client == null) {
            return;
        }
        try {
            client.delete(request -> request
                    .index(indexName())
                    .id(String.valueOf(id))
                    .refresh(Refresh.WaitFor));
        } catch (Exception ignored) {
        }
    }

    public SearchResponse search(String q, int size, String tagsCsv, String after, Long currentUserIdNullable) {
        String keyword = normalizeKeyword(q);
        if (keyword.isBlank()) {
            return new SearchResponse(List.of(), null, false);
        }

        int safeSize = normalizeSize(size);
        ElasticsearchClient client = elasticsearchClient();
        if (client != null) {
            try {
                return searchWithElasticsearch(client, keyword, safeSize, tagsCsv, after);
            } catch (Exception ignored) {
            }
        }
        return searchInMemory(keyword, safeSize, tagsCsv, after);
    }

    public SuggestResponse suggest(String prefix, int size) {
        ElasticsearchClient client = elasticsearchClient();
        if (client != null) {
            try {
                return suggestWithElasticsearch(client, prefix, normalizeSize(size));
            } catch (Exception ignored) {
            }
        }
        return suggestInMemory(prefix, normalizeSize(size));
    }

    private void ensureIndex() {
        ElasticsearchClient client = elasticsearchClient();
        if (client == null) {
            return;
        }
        try {
            boolean exists = client.indices().exists(request -> request.index(indexName())).value();
            if (exists) {
                return;
            }
            client.indices().create(create -> create.index(indexName()).mappings(mapping -> mapping
                    .properties("content_id", property -> property.long_(number -> number))
                    .properties("title", property -> property.text(text -> text))
                    .properties("description", property -> property.text(text -> text))
                    .properties("body", property -> property.text(text -> text))
                    .properties("tags", property -> property.keyword(keyword -> keyword))
                    .properties("img_urls", property -> property.keyword(keyword -> keyword))
                    .properties("author_avatar", property -> property.keyword(keyword -> keyword))
                    .properties("author_nickname", property -> property.keyword(keyword -> keyword))
                    .properties("author_tag_json", property -> property.keyword(keyword -> keyword))
                    .properties("status", property -> property.keyword(keyword -> keyword))
                    .properties("visible", property -> property.keyword(keyword -> keyword))
                    .properties("is_top", property -> property.boolean_(bool -> bool))
                    .properties("publish_time", property -> property.date(date -> date))
                    .properties("title_suggest", property -> property.completion(completion -> completion))));
        } catch (Exception ignored) {
        }
    }

    private void rebuildFromDatabase() {
        inMemoryDocuments.clear();
        int limit = 200;
        int offset = 0;
        while (true) {
            List<KnowPostFeedRow> rows = knowPostMapper.listFeedPublic(limit, offset);
            if (rows == null || rows.isEmpty()) {
                return;
            }
            for (KnowPostFeedRow row : rows) {
                upsertKnowPost(row.getId());
            }
            offset += rows.size();
        }
    }

    private SearchDocument buildDocument(long id) {
        KnowPostDetailRow row = knowPostMapper.findDetailById(id);
        if (row == null) {
            return null;
        }
        if (!"published".equalsIgnoreCase(row.getStatus()) || !"public".equalsIgnoreCase(row.getVisible())) {
            return null;
        }

        return new SearchDocument(
                row.getId(),
                row.getTitle(),
                row.getDescription(),
                loadContentBody(row.getContentUrl(), row.getDescription()),
                parseStringArray(row.getTags()),
                parseStringArray(row.getImgUrls()),
                row.getAuthorAvatar(),
                row.getAuthorNickname(),
                row.getAuthorTagJson(),
                row.getStatus(),
                row.getVisible(),
                row.getIsTop(),
                row.getPublishTime(),
                row.getTitle()
        );
    }

    private String loadContentBody(String contentUrl, String fallback) {
        String content = knowPostContentLoader.loadContent(contentUrl);
        if (content == null || content.isBlank()) {
            return fallback == null ? "" : fallback;
        }
        return truncate(content, MAX_BODY_LENGTH);
    }

    private SearchResponse searchInMemory(String keyword, int size, String tagsCsv, String after) {
        List<String> tags = parseCsv(tagsCsv);
        SearchCursor cursor = decodeCursor(after);
        Comparator<SearchDocument> comparator = searchComparator(keyword);

        List<SearchDocument> matched = inMemoryDocuments.values().stream()
                .filter(document -> "published".equals(document.status()) && "public".equals(document.visible()))
                .filter(document -> tags == null || tags.isEmpty() || document.tags().stream().anyMatch(tags::contains))
                .filter(document -> matches(document, keyword))
                .filter(document -> cursor == null || compareCursor(cursorFor(document, keyword), cursor) > 0)
                .sorted(comparator)
                .limit(size + 1L)
                .toList();

        boolean hasMore = matched.size() > size;
        List<SearchDocument> page = hasMore ? matched.subList(0, size) : matched;
        String nextAfter = hasMore && !page.isEmpty() ? encodeCursor(cursorFor(page.getLast(), keyword)) : null;

        List<FeedItemResponse> items = page.stream()
                .map(document -> toFeedItem(document, resolveDescription(document, keyword)))
                .toList();
        return new SearchResponse(items, nextAfter, hasMore);
    }

    @SuppressWarnings("unchecked")
    private SearchResponse searchWithElasticsearch(
            ElasticsearchClient client,
            String keyword,
            int size,
            String tagsCsv,
            String after
    ) throws Exception {
        List<String> tags = parseCsv(tagsCsv);
        SearchCursor cursor = decodeCursor(after);

        co.elastic.clients.elasticsearch.core.SearchResponse<Map<String, Object>> response = client.search(search -> {
            var builder = search.index(indexName())
                    .size(size + 1)
                    .query(query -> query.bool(bool -> {
                        bool.must(must -> must.multiMatch(multi -> multi.query(keyword).fields("title^3", "description", "body")));
                        bool.filter(filter -> filter.term(term -> term.field("status").value(value -> value.stringValue("published"))));
                        bool.filter(filter -> filter.term(term -> term.field("visible").value(value -> value.stringValue("public"))));
                        if (tags != null && !tags.isEmpty()) {
                            bool.filter(filter -> filter.terms(terms -> terms.field("tags")
                                    .terms(values -> values.value(tags.stream().map(FieldValue::of).toList()))));
                        }
                        return bool;
                    }))
                    .highlight(highlight -> highlight
                            .fields("title", field -> field.numberOfFragments(0))
                            .fields("body", field -> field.fragmentSize(120).numberOfFragments(1)))
                    .sort(sort -> sort.score(score -> score.order(SortOrder.Desc)))
                    .sort(sort -> sort.field(field -> field.field("publish_time").order(SortOrder.Desc)))
                    .sort(sort -> sort.field(field -> field.field("content_id").order(SortOrder.Desc)));
            if (cursor != null) {
                builder = builder.searchAfter(cursor.toFieldValues());
            }
            return builder;
        }, (Class<Map<String, Object>>) (Class<?>) Map.class);

        List<Hit<Map<String, Object>>> hits = response.hits() == null ? List.of() : response.hits().hits();
        boolean hasMore = hits.size() > size;
        List<Hit<Map<String, Object>>> page = hasMore ? hits.subList(0, size) : hits;
        String nextAfter = hasMore && !page.isEmpty() ? encodeCursor(cursorFromHit(page.getLast())) : null;

        List<FeedItemResponse> items = page.stream()
                .map(hit -> {
                    Map<String, Object> source = hit.source();
                    if (source == null) {
                        return null;
                    }
                    return toFeedItem(source, buildSnippet(hit));
                })
                .filter(Objects::nonNull)
                .toList();
        return new SearchResponse(items, nextAfter, hasMore);
    }

    @SuppressWarnings("unchecked")
    private SuggestResponse suggestWithElasticsearch(ElasticsearchClient client, String prefix, int size) throws Exception {
        co.elastic.clients.elasticsearch.core.SearchResponse<Map<String, Object>> response = client.search(search -> search
                        .index(indexName())
                        .suggest(suggest -> suggest.suggesters("title_suggest",
                                suggester -> suggester.prefix(prefix).completion(completion -> completion.field("title_suggest").size(size)))),
                (Class<Map<String, Object>>) (Class<?>) Map.class);

        List<String> items = new ArrayList<>();
        var suggestions = response.suggest();
        if (suggestions != null) {
            var entries = suggestions.get("title_suggest");
            if (entries != null) {
                for (var entry : entries) {
                    var completion = entry.completion();
                    if (completion != null && completion.options() != null) {
                        for (var option : completion.options()) {
                            if (option.text() != null && !option.text().isBlank()) {
                                items.add(option.text());
                            }
                        }
                    }
                }
            }
        }
        return new SuggestResponse(items);
    }

    private SuggestResponse suggestInMemory(String prefix, int size) {
        String normalized = prefix == null ? "" : prefix.trim().toLowerCase();
        List<String> items = inMemoryDocuments.values().stream()
                .filter(document -> "published".equals(document.status()) && "public".equals(document.visible()))
                .map(SearchDocument::titleSuggest)
                .filter(Objects::nonNull)
                .filter(title -> title.toLowerCase().startsWith(normalized))
                .distinct()
                .sorted()
                .limit(size)
                .toList();
        return new SuggestResponse(items);
    }

    private FeedItemResponse toFeedItem(SearchDocument document, String description) {
        return new FeedItemResponse(
                String.valueOf(document.contentId()),
                document.title(),
                description,
                document.imgUrls().isEmpty() ? null : document.imgUrls().get(0),
                document.tags(),
                document.authorAvatar(),
                document.authorNickname(),
                document.authorTagJson(),
                0L,
                0L,
                null,
                null,
                document.isTop()
        );
    }

    private FeedItemResponse toFeedItem(Map<String, Object> source, String snippet) {
        String description = snippet == null || snippet.isBlank()
                ? asString(source.get("description"))
                : snippet;
        return new FeedItemResponse(
                asString(source.get("content_id")),
                asString(source.get("title")),
                description,
                asStringList(source.get("img_urls")).stream().findFirst().orElse(null),
                asStringList(source.get("tags")),
                asString(source.get("author_avatar")),
                asString(source.get("author_nickname")),
                asString(source.get("author_tag_json")),
                0L,
                0L,
                null,
                null,
                asBoolean(source.get("is_top"))
        );
    }

    private String resolveDescription(SearchDocument document, String keyword) {
        String snippet = buildSnippet(document, keyword);
        if (snippet != null && !snippet.isBlank()) {
            return snippet;
        }
        return document.description();
    }

    private String buildSnippet(SearchDocument document, String keyword) {
        if (!containsIgnoreCase(document.body(), keyword)) {
            return null;
        }
        return extractBodySnippet(document.body(), keyword);
    }

    private String buildSnippet(Hit<Map<String, Object>> hit) {
        if (hit.highlight() == null) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        List<String> titleParts = hit.highlight().get("title");
        if (titleParts != null && !titleParts.isEmpty()) {
            parts.add(String.join(" ", titleParts));
        }
        List<String> bodyParts = hit.highlight().get("body");
        if (bodyParts != null && !bodyParts.isEmpty()) {
            parts.add(String.join(" ", bodyParts));
        }
        if (parts.isEmpty()) {
            return null;
        }
        return String.join(" ", parts);
    }

    private String extractBodySnippet(String body, String keyword) {
        if (body == null || body.isBlank() || keyword.isBlank()) {
            return null;
        }
        String bodyLower = body.toLowerCase();
        String keywordLower = keyword.toLowerCase();
        int match = bodyLower.indexOf(keywordLower);
        if (match < 0) {
            return null;
        }
        int start = Math.max(0, match - BODY_SNIPPET_RADIUS);
        int end = Math.min(body.length(), match + keyword.length() + BODY_SNIPPET_RADIUS);
        String snippet = body.substring(start, end).replaceAll("\\s+", " ").trim();
        if (start > 0) {
            snippet = "..." + snippet;
        }
        if (end < body.length()) {
            snippet = snippet + "...";
        }
        return snippet;
    }

    private boolean matches(SearchDocument document, String keyword) {
        return containsIgnoreCase(document.title(), keyword)
                || containsIgnoreCase(document.description(), keyword)
                || containsIgnoreCase(document.body(), keyword);
    }

    private int score(SearchDocument document, String keyword) {
        int score = 0;
        if (containsIgnoreCase(document.title(), keyword)) {
            score += 3;
        }
        if (containsIgnoreCase(document.description(), keyword)) {
            score += 1;
        }
        if (containsIgnoreCase(document.body(), keyword)) {
            score += 1;
        }
        return score;
    }

    private Comparator<SearchDocument> searchComparator(String keyword) {
        return Comparator
                .comparingInt((SearchDocument document) -> score(document, keyword)).reversed()
                .thenComparing((SearchDocument document) -> normalizePublishTime(document.publishTime()), Comparator.reverseOrder())
                .thenComparing((SearchDocument document) -> document.contentId() == null ? Long.MIN_VALUE : document.contentId(), Comparator.reverseOrder());
    }

    private SearchCursor cursorFor(SearchDocument document, String keyword) {
        return new SearchCursor(
                score(document, keyword),
                normalizePublishTime(document.publishTime()),
                document.contentId() == null ? Long.MIN_VALUE : document.contentId()
        );
    }

    private SearchCursor cursorFromHit(Hit<Map<String, Object>> hit) {
        List<FieldValue> sortValues = hit.sort();
        if (sortValues == null || sortValues.size() < 3) {
            return null;
        }
        return new SearchCursor(
                fieldValueAsDouble(sortValues.get(0)),
                fieldValueAsLong(sortValues.get(1)),
                fieldValueAsLong(sortValues.get(2))
        );
    }

    private int compareCursor(SearchCursor left, SearchCursor right) {
        int byScore = Double.compare(right.score(), left.score());
        if (byScore != 0) {
            return byScore;
        }
        int byPublishTime = Long.compare(right.publishTime(), left.publishTime());
        if (byPublishTime != 0) {
            return byPublishTime;
        }
        return Long.compare(right.contentId(), left.contentId());
    }

    private SearchCursor decodeCursor(String after) {
        if (after == null || after.isBlank()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(after), StandardCharsets.UTF_8);
            return objectMapper.readValue(decoded, SearchCursor.class);
        } catch (Exception exception) {
            return null;
        }
    }

    private String encodeCursor(SearchCursor cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(cursor);
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            return null;
        }
    }

    private List<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return null;
        }
        return List.of(csv.split(",")).stream()
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());
    }

    private List<String> parseStringArray(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception exception) {
            return Collections.emptyList();
        }
    }

    private String normalizeKeyword(String value) {
        return value == null ? "" : value.trim();
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private long normalizePublishTime(Instant publishTime) {
        return publishTime == null ? Long.MIN_VALUE : publishTime.toEpochMilli();
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && !keyword.isBlank() && value.toLowerCase().contains(keyword.toLowerCase());
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private List<String> asStringList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof List<?> list) {
            return list.stream().filter(Objects::nonNull).map(String::valueOf).toList();
        }
        String text = String.valueOf(value).trim();
        if (text.startsWith("[") && text.endsWith("]")) {
            text = text.substring(1, text.length() - 1);
            if (text.isBlank()) {
                return Collections.emptyList();
            }
            return List.of(text.split(",")).stream()
                    .map(String::trim)
                    .map(item -> item.replace("\"", ""))
                    .filter(item -> !item.isBlank())
                    .toList();
        }
        return Collections.emptyList();
    }

    private double fieldValueAsDouble(FieldValue value) {
        if (value.isDouble()) {
            return value.doubleValue();
        }
        if (value.isLong()) {
            return value.longValue();
        }
        if (value.isString()) {
            try {
                return Double.parseDouble(value.stringValue());
            } catch (Exception ignored) {
                return 0D;
            }
        }
        return 0D;
    }

    private long fieldValueAsLong(FieldValue value) {
        if (value.isLong()) {
            return value.longValue();
        }
        if (value.isDouble()) {
            return Math.round(value.doubleValue());
        }
        if (value.isString()) {
            try {
                return Long.parseLong(value.stringValue());
            } catch (Exception ignored) {
                return Long.MIN_VALUE;
            }
        }
        return Long.MIN_VALUE;
    }

    private ElasticsearchClient elasticsearchClient() {
        if (!elasticsearchProperties.isEnabled()) {
            return null;
        }
        return elasticsearchClientProvider.getIfAvailable();
    }

    private String indexName() {
        return elasticsearchProperties.getIndexName();
    }

    private record SearchCursor(double score, long publishTime, long contentId) {
        private List<FieldValue> toFieldValues() {
            return List.of(
                    FieldValue.of(score),
                    FieldValue.of(publishTime),
                    FieldValue.of(contentId)
            );
        }
    }

    private record SearchDocument(
            Long contentId,
            String title,
            String description,
            String body,
            List<String> tags,
            List<String> imgUrls,
            String authorAvatar,
            String authorNickname,
            String authorTagJson,
            String status,
            String visible,
            Boolean isTop,
            Instant publishTime,
            String titleSuggest
    ) {
        private Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("content_id", contentId);
            map.put("title", title == null ? "" : title);
            map.put("description", description == null ? "" : description);
            map.put("body", body == null ? "" : body);
            map.put("tags", tags == null ? List.of() : tags);
            map.put("img_urls", imgUrls == null ? List.of() : imgUrls);
            map.put("author_avatar", authorAvatar);
            map.put("author_nickname", authorNickname);
            map.put("author_tag_json", authorTagJson);
            map.put("status", status);
            map.put("visible", visible);
            map.put("is_top", isTop);
            map.put("publish_time", publishTime == null ? null : publishTime.toEpochMilli());
            map.put("title_suggest", titleSuggest);
            return map;
        }
    }
}

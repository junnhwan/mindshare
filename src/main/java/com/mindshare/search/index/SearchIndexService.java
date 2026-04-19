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

    private final ObjectProvider<ElasticsearchClient> elasticsearchClientProvider;
    private final ElasticsearchConfig.MindShareElasticsearchProperties elasticsearchProperties;
    private final KnowPostMapper knowPostMapper;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<Long, SearchDocument> inMemoryDocuments = new ConcurrentHashMap<>();

    public SearchIndexService(
            ObjectProvider<ElasticsearchClient> elasticsearchClientProvider,
            ElasticsearchConfig.MindShareElasticsearchProperties elasticsearchProperties,
            KnowPostMapper knowPostMapper,
            ObjectMapper objectMapper
    ) {
        this.elasticsearchClientProvider = elasticsearchClientProvider;
        this.elasticsearchProperties = elasticsearchProperties;
        this.knowPostMapper = knowPostMapper;
        this.objectMapper = objectMapper;
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
        ElasticsearchClient client = elasticsearchClient();
        if (client != null) {
            try {
                return searchWithElasticsearch(client, q, size, tagsCsv, after);
            } catch (Exception ignored) {
            }
        }
        return searchInMemory(q, size, tagsCsv, after);
    }

    public SuggestResponse suggest(String prefix, int size) {
        ElasticsearchClient client = elasticsearchClient();
        if (client != null) {
            try {
                return suggestWithElasticsearch(client, prefix, size);
            } catch (Exception ignored) {
            }
        }
        return suggestInMemory(prefix, size);
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
        return new SearchDocument(
                row.getId(),
                row.getTitle(),
                row.getDescription(),
                row.getDescription(),
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

    private SearchResponse searchInMemory(String q, int size, String tagsCsv, String after) {
        String keyword = q == null ? "" : q.trim().toLowerCase();
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = decodeAfter(after);
        List<String> tags = parseCsv(tagsCsv);

        List<SearchDocument> matched = inMemoryDocuments.values().stream()
                .filter(document -> "published".equals(document.status()) && "public".equals(document.visible()))
                .filter(document -> tags == null || tags.isEmpty() || document.tags().stream().anyMatch(tags::contains))
                .filter(document -> matches(document, keyword))
                .sorted(Comparator
                        .comparingInt((SearchDocument document) -> score(document, keyword)).reversed()
                        .thenComparing(SearchDocument::publishTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(SearchDocument::contentId, Comparator.reverseOrder()))
                .toList();

        int toIndex = Math.min(offset + safeSize, matched.size());
        List<SearchDocument> page = offset >= matched.size() ? List.of() : matched.subList(offset, toIndex);
        boolean hasMore = toIndex < matched.size();
        String nextAfter = hasMore ? encodeAfter(toIndex) : null;

        List<FeedItemResponse> items = page.stream()
                .map(this::toFeedItem)
                .toList();
        return new SearchResponse(items, nextAfter, hasMore);
    }

    @SuppressWarnings("unchecked")
    private SearchResponse searchWithElasticsearch(ElasticsearchClient client, String q, int size, String tagsCsv, String after) throws Exception {
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = decodeAfter(after);
        List<String> tags = parseCsv(tagsCsv);

        co.elastic.clients.elasticsearch.core.SearchResponse<Map<String, Object>> response = client.search(search -> {
            var builder = search.index(indexName())
                    .from(offset)
                    .size(safeSize + 1)
                    .query(query -> query.bool(bool -> {
                        bool.must(must -> must.multiMatch(multi -> multi.query(q).fields("title^3", "description", "body")));
                        bool.filter(filter -> filter.term(term -> term.field("status").value(value -> value.stringValue("published"))));
                        bool.filter(filter -> filter.term(term -> term.field("visible").value(value -> value.stringValue("public"))));
                        if (tags != null && !tags.isEmpty()) {
                            bool.filter(filter -> filter.terms(terms -> terms.field("tags")
                                    .terms(values -> values.value(tags.stream().map(FieldValue::of).toList()))));
                        }
                        return bool;
                    }))
                    .sort(sort -> sort.field(field -> field.field("publish_time").order(SortOrder.Desc)))
                    .sort(sort -> sort.field(field -> field.field("content_id").order(SortOrder.Desc)));
            return builder;
        }, (Class<Map<String, Object>>) (Class<?>) Map.class);

        List<Hit<Map<String, Object>>> hits = response.hits() == null ? List.of() : response.hits().hits();
        boolean hasMore = hits.size() > safeSize;
        if (hasMore) {
            hits = hits.subList(0, safeSize);
        }
        List<FeedItemResponse> items = hits.stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(this::toFeedItem)
                .toList();
        String nextAfter = hasMore ? encodeAfter(offset + items.size()) : null;
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
                .limit(Math.max(size, 1))
                .toList();
        return new SuggestResponse(items);
    }

    private FeedItemResponse toFeedItem(SearchDocument document) {
        return new FeedItemResponse(
                String.valueOf(document.contentId()),
                document.title(),
                document.description(),
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

    private FeedItemResponse toFeedItem(Map<String, Object> source) {
        return new FeedItemResponse(
                asString(source.get("content_id")),
                asString(source.get("title")),
                asString(source.get("description")),
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

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && !keyword.isBlank() && value.toLowerCase().contains(keyword);
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

    private int decodeAfter(String after) {
        if (after == null || after.isBlank()) {
            return 0;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(after), StandardCharsets.UTF_8);
            return Integer.parseInt(decoded);
        } catch (Exception exception) {
            return 0;
        }
    }

    private String encodeAfter(int offset) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(String.valueOf(offset).getBytes(StandardCharsets.UTF_8));
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

    private ElasticsearchClient elasticsearchClient() {
        if (!elasticsearchProperties.isEnabled()) {
            return null;
        }
        return elasticsearchClientProvider.getIfAvailable();
    }

    private String indexName() {
        return elasticsearchProperties.getIndexName();
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

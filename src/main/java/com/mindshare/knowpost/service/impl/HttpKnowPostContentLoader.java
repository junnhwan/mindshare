package com.mindshare.knowpost.service.impl;

import com.mindshare.knowpost.service.KnowPostContentLoader;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Profile("!bootstrap-test")
public class HttpKnowPostContentLoader implements KnowPostContentLoader {

    private static final Pattern CHARSET_PATTERN =
            Pattern.compile("charset\\s*=\\s*['\\\"]?([a-zA-Z0-9_\\-]+)", Pattern.CASE_INSENSITIVE);

    @Override
    public String loadContent(String contentUrl) {
        if (contentUrl == null || contentUrl.isBlank()) {
            return null;
        }

        try {
            URI uri = URI.create(contentUrl);
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                return Files.readString(Path.of(uri));
            }
        } catch (Exception ignored) {
        }

        try {
            URLConnection connection = new URL(contentUrl).openConnection();
            byte[] bytes;
            try (var inputStream = connection.getInputStream()) {
                bytes = inputStream.readAllBytes();
            }
            if (bytes.length == 0) {
                return null;
            }
            Charset charset = pickCharset(bytes, parseCharset(connection.getContentType()), sniffHtmlCharset(bytes));
            return new String(bytes, charset);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Charset parseCharset(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        Matcher matcher = CHARSET_PATTERN.matcher(contentType);
        if (!matcher.find()) {
            return null;
        }
        return normalizeCharset(matcher.group(1));
    }

    private Charset pickCharset(byte[] bytes, Charset headerCharset, Charset metaCharset) {
        if (metaCharset != null) {
            return metaCharset;
        }
        Charset utf8 = StandardCharsets.UTF_8;
        Charset gb18030 = Charset.forName("GB18030");
        if (headerCharset == null) {
            return countReplacementChars(new String(bytes, utf8)) <= countReplacementChars(new String(bytes, gb18030))
                    ? utf8
                    : gb18030;
        }
        if (StandardCharsets.ISO_8859_1.equals(headerCharset) || StandardCharsets.US_ASCII.equals(headerCharset)) {
            int headerReplacements = countReplacementChars(new String(bytes, headerCharset));
            int utf8Replacements = countReplacementChars(new String(bytes, utf8));
            int gbReplacements = countReplacementChars(new String(bytes, gb18030));
            if (utf8Replacements <= gbReplacements && utf8Replacements <= headerReplacements) {
                return utf8;
            }
            if (gbReplacements <= headerReplacements) {
                return gb18030;
            }
        }
        return headerCharset;
    }

    private Charset sniffHtmlCharset(byte[] bytes) {
        int limit = Math.min(bytes.length, 8192);
        String head = new String(bytes, 0, limit, StandardCharsets.ISO_8859_1);
        Matcher matcher = CHARSET_PATTERN.matcher(head);
        if (!matcher.find()) {
            return null;
        }
        return normalizeCharset(matcher.group(1));
    }

    private Charset normalizeCharset(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if ("utf8".equalsIgnoreCase(value)) {
            return StandardCharsets.UTF_8;
        }
        if ("gbk".equalsIgnoreCase(value)
                || "gb2312".equalsIgnoreCase(value)
                || "gb18030".equalsIgnoreCase(value)) {
            return Charset.forName("GB18030");
        }
        try {
            return Charset.forName(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private int countReplacementChars(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) == '\uFFFD') {
                count++;
            }
        }
        return count;
    }
}

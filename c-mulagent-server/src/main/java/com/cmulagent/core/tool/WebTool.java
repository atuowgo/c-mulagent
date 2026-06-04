package com.cmulagent.core.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Built-in tool for web operations: WEB_FETCH and WEB_SEARCH.
 */
public class WebTool {

    private static final Logger log = LoggerFactory.getLogger(WebTool.class);

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient;

    public WebTool() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public ToolExecutor fetchExecutor() {
        return params -> {
            try {
                String url = (String) params.get("url");
                if (url == null || url.isBlank()) {
                    return "Error: 'url' parameter is required";
                }

                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(TIMEOUT)
                        .header("User-Agent", "CMulagent/1.0")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                int statusCode = response.statusCode();
                if (statusCode >= 400) {
                    return "HTTP " + statusCode + " for URL: " + url;
                }

                String contentType = response.headers().firstValue("Content-Type").orElse("");
                if (!contentType.contains("text/html") && !contentType.contains("text/plain")) {
                    return "Fetched " + url + " (" + contentType + ", " + response.body().length() + " bytes)";
                }

                String body = stripHtml(response.body());
                if (body.length() > 10000) {
                    body = body.substring(0, 10000) + "\n... (truncated at 10000 characters)";
                }
                return body;

            } catch (Exception e) {
                log.error("WEB_FETCH failed for URL: {}", params.get("url"), e);
                return "Error fetching URL: " + e.getMessage();
            }
        };
    }

    public ToolExecutor searchExecutor() {
        return params -> "Web search is not configured. Use WEB_FETCH to fetch specific URLs.";
    }

    private String stripHtml(String html) {
        if (html == null) return "";

        // Remove scripts and styles
        String result = html.replaceAll("(?is)<script[^>]*>.*?</script>", " ");
        result = result.replaceAll("(?is)<style[^>]*>.*?</style>", " ");

        // Remove HTML tags
        result = result.replaceAll("<[^>]+>", " ");

        // Decode common HTML entities
        result = result.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ");

        // Collapse whitespace
        result = result.replaceAll("\\s+", " ").trim();

        return result;
    }
}
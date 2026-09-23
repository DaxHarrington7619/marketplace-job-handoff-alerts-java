package com.example.marketplace;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public final class InfraiClient implements ErrorReporter {
    // Infrai capability: errors.capture
    private final HttpClient http = HttpClient.newHttpClient();
    private final String key;
    private final String baseUrl;

    public InfraiClient(String key, String baseUrl) {
        this.key = key;
        this.baseUrl = baseUrl;
    }

    public void capture(String title, String message, String exception, Map<String, String> context) throws IOException, InterruptedException {
        String json = "{\"title\":\"" + esc(title) + "\",\"message\":\"" + esc(message)
                + "\",\"level\":\"error\",\"exception\":\"" + esc(exception)
                + "\",\"context\":{\"order_id\":\"" + esc(context.get("order_id"))
                + "\",\"seller_asset_id\":\"" + esc(context.get("seller_asset_id")) + "\"}}";
        request("POST", "/v1/errors/capture", json);
    }

    private void request(String method, String path, String body) throws IOException, InterruptedException {
        int attempt = 0;
        while (true) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (response.statusCode() == 429 && attempt < 3) {
                long wait = Math.min(4000L, 250L * (1L << attempt));
                String retryAfter = response.headers().firstValue("Retry-After").orElse("");
                if (retryAfter.matches("\\d+")) wait = Long.parseLong(retryAfter) * 1000L;
                Thread.sleep(wait);
                attempt++;
                continue;
            }
            if (!envelope.contains("\"ok\":true")) throw new IOException("Infrai request rejected: " + envelope);
            if (response.statusCode() >= 500) throw new IOException("Infrai transport failure: HTTP " + response.statusCode());
            return;
        }
    }

    private static String esc(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"); }
}

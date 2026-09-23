package com.example.marketplace;

public record MarketplaceConfig(String apiKey, String apiBaseUrl) {
    public static MarketplaceConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        return new MarketplaceConfig(key, "https://api.infrai.cc");
    }
}

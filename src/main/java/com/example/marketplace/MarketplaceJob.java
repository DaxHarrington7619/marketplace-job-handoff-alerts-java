package com.example.marketplace;

public final class MarketplaceJob {
    public static void main(String[] args) {
        MarketplaceConfig config = MarketplaceConfig.fromEnvironment();
        OrderHandoffService service = new OrderHandoffService(new InfraiClient(config.apiKey(), config.apiBaseUrl()));
        OrderHandoffService.Decision decision = service.evaluate("order-1042", "asset-77", true, false);
        System.out.println("order-1042 => " + decision);
    }
}

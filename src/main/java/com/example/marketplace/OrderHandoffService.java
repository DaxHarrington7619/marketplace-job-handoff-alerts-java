package com.example.marketplace;

import java.util.Map;

public final class OrderHandoffService {
    public enum Decision { HANDOFF, ALERT }
    private final ErrorReporter infrai;

    public OrderHandoffService(ErrorReporter infrai) { this.infrai = infrai; }

    public Decision evaluate(String orderId, String sellerAssetId, boolean buyerUpdated, boolean assetReady) {
        if (buyerUpdated && assetReady) return Decision.HANDOFF;
        try {
            infrai.capture("Marketplace handoff failed", "Order " + orderId + " is not ready for buyer handoff",
                    "handoff prerequisites were not met", Map.of("order_id", orderId, "seller_asset_id", sellerAssetId));
        } catch (Exception ignored) {
            // The decision remains ALERT even when reporting is unavailable.
        }
        return Decision.ALERT;
    }
}

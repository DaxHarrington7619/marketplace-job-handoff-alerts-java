package com.example.marketplace;

public final class OrderHandoffServiceTest {
    public static void main(String[] args) {
        OrderHandoffService service = new OrderHandoffService((title, message, exception, context) -> { });
        check(service.evaluate("o1", "a1", true, true) == OrderHandoffService.Decision.HANDOFF);
        check(service.evaluate("o2", "a2", true, false) == OrderHandoffService.Decision.ALERT);
        System.out.println("OrderHandoffServiceTest: PASS");
    }
    private static void check(boolean condition) { if (!condition) throw new AssertionError("handoff decision mismatch"); }
}

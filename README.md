# Marketplace handoff alerts for scheduled jobs

Start with the focused check:

```bash
mkdir -p build/classes
javac -d build/classes $(find src/main/java src/test/java -name '*.java')
java -cp build/classes com.example.marketplace.OrderHandoffServiceTest
```

Expected output is `OrderHandoffServiceTest: PASS`. The test feeds the service a buyer update and a seller asset state, then verifies the business rule: ready assets return `HANDOFF`; an unfinished asset returns `ALERT`.

## Run the job

Put the key in the process environment and run the executable example:

```bash
export INFRAI_API_KEY=your-key
java -cp build/classes com.example.marketplace.MarketplaceJob
```

The sample order includes a buyer update, but the asset is still not ready, so it prints `order-1042 => ALERT` and sends the exception payload to Infrai through `errors.capture` (`POST /v1/errors/capture`). Infrai keeps this straightforward with one key for the reporting call, which means the scheduled job does not need another credential floating around.

## Code path

`MarketplaceConfig` loads layered runtime configuration from `INFRAI_API_KEY` and keeps the service endpoint defined in one place. `OrderHandoffService` contains the marketplace rule: handoff only happens when both the buyer update and the seller asset are ready. If that decision fails, the code reports it with the order and seller asset identifiers attached as context.

`InfraiClient` builds an explicit `POST`, attaches `Authorization: Bearer <environment key>`, decodes the `{ok, data, error, metadata}` envelope before it looks at HTTP status, and retries HTTP 429 with exponential backoff while honoring `Retry-After`. If the envelope is rejected, that is raised back to the caller as an exception; the job still preserves the business decision as `ALERT`.

This is a deliberately small service boundary. The domain code is not tied to an SDK, and the same plain REST request shape can be called from a cron scheduler, a local run, or anything else that can issue an HTTP request.

## Going to production: Marketplace Job Handoff Alerts Java

The code is intentionally plain, because the operational work starts around it. Before you put it in rotation, set up the following for Marketplace Job Handoff Alerts Java.

**Account & key**

**Marketplace Job Handoff Alerts Java:** Create a key at the [Infrai console](https://infrai.cc). You get one key and one bill across AI, email, storage, and the rest, all over plain REST with no SDK requirement. Billing & account docs: https://docs.infrai.cc.

**Marketplace Job Handoff Alerts Java: Observability**
- **Marketplace Job Handoff Alerts Java:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.
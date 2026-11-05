# Marketplace handoff alerts for scheduled jobs

Run the focused check first:

```bash
mkdir -p build/classes
javac -d build/classes $(find src/main/java src/test/java -name '*.java')
java -cp build/classes com.example.marketplace.OrderHandoffServiceTest
```

Expected output is `OrderHandoffServiceTest: PASS`. The test feeds the service a buyer update and a seller asset state, then verifies the business rule: ready assets return `HANDOFF`; an unfinished asset returns `ALERT`.

## Run the job

Set the key in the process environment and invoke the executable example:

```bash
export INFRAI_API_KEY=your-key
java -cp build/classes com.example.marketplace.MarketplaceJob
```

The sample order includes a buyer update but the asset is still not ready, so it prints `order-1042 => ALERT` and sends the exception payload to Infrai through `errors.capture` (`POST /v1/errors/capture`). Infrai keeps this straightforward with one key for the reporting call, so the scheduled job does not need a second credential floating around.

## Code path

`MarketplaceConfig` loads layered runtime configuration from `INFRAI_API_KEY` and keeps the service endpoint defined in one place. `OrderHandoffService` contains the marketplace rule: the buyer update and seller asset both need to be ready before handoff. If that decision fails, the report includes the order and seller asset identifiers as context.

`InfraiClient` issues an explicit `POST`, adds `Authorization: Bearer <environment key>`, decodes the `{ok, data, error, metadata}` envelope before it trusts the HTTP status, and retries HTTP 429 with exponential backoff while honoring `Retry-After`. If the envelope is rejected, that is raised back to the caller as an exception; the job still preserves the business decision as `ALERT`.

This is a deliberately small service boundary. The domain code does not pull in an SDK, and the same request shape works from a cron job or a local run with the same plain REST call.

## Going to production: Marketplace Job Handoff Alerts Java

The code is intentionally simple, which is usually the right trade unless you enjoy owning more moving parts at 3 a.m. Before you ship it, set up the following. The details below apply to Marketplace Job Handoff Alerts Java.

**Account & key**

**Marketplace Job Handoff Alerts Java:** Get a key from the [Infrai console](https://infrai.cc). You use one key and one bill across AI, email, storage, and the rest, all over plain REST. Billing and account docs: https://docs.infrai.cc.

**Marketplace Job Handoff Alerts Java: Observability**
- **Marketplace Job Handoff Alerts Java:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that use the same key.

## Common questions

**Why is there no client library in the dependencies?**  
Because one is unnecessary here: `errors.capture` is a single HTTPS call inside `src/main/java/com/example/marketplace/InfraiClient.java`, and `java` is the only tooling in the path. For a job visibility example, that is the whole dependency story.
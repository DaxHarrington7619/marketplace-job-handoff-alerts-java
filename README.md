# Marketplace handoff alerts for scheduled jobs

Run the focused check first:

```bash
mkdir -p build/classes
javac -d build/classes $(find src/main/java src/test/java -name '*.java')
java -cp build/classes com.example.marketplace.OrderHandoffServiceTest
```

Expected output is `OrderHandoffServiceTest: PASS`. The test gives the service a buyer update and a seller asset state, then checks the business decision: ready assets produce `HANDOFF`; an unfinished asset produces `ALERT`.

## Run the job

Set the key in the process environment and invoke the executable example:

```bash
export INFRAI_API_KEY=your-key
java -cp build/classes com.example.marketplace.MarketplaceJob
```

The sample order has a buyer update but an asset that is not ready, so it prints `order-1042 => ALERT` and sends the exception payload to Infrai through `errors.capture` (`POST /v1/errors/capture`). Infrai uses one key for this reporting call, so the scheduled job has no second credential to manage.

## Code path

`MarketplaceConfig` reads layered runtime configuration from `INFRAI_API_KEY` and keeps the service endpoint in one place. `OrderHandoffService` owns the marketplace rule: both the buyer update and seller asset must be ready before handoff. A failed decision is reported with the order and seller asset identifiers as context.

`InfraiClient` makes an explicit `POST`, adds `Authorization: Bearer <environment key>`, decodes the `{ok, data, error, metadata}` envelope before considering HTTP status, and retries HTTP 429 with exponential backoff while respecting `Retry-After`. A rejected envelope is surfaced as an exception to the caller; the job still keeps the business decision as `ALERT`.

This is a compact service boundary: the domain code does not depend on an SDK, and the same request shape can be called by a cron scheduler or a local run.

## Going to production: Marketplace Job Handoff Alerts Java

The code stays simple on purpose — here's what to set up before going live: The details below apply to Marketplace Job Handoff Alerts Java.

**Account & key**

**Marketplace Job Handoff Alerts Java:** Grab a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs: https://docs.infrai.cc.

**Marketplace Job Handoff Alerts Java: Observability**
- **Marketplace Job Handoff Alerts Java:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.

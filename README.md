# API Monitoring & Observability Platform

> Kotlin + Next.js reference implementation for capturing API telemetry, surfacing health insights, and demoing alert workflows end to end.

## Milestone Checklist
- [x] Milestone 0 – Project Skeleton & docker-compose
- [x] Milestone 1 – API Tracking Client
- [x] Milestone 2 – Collector Service
- [x] Milestone 3 – Next.js Dashboard
- [x] Milestone 4 – Non-functional Requirements
- [x] Milestone 5 – Final Delivery

## Environment Variables
Define these in a `.env` file (docker-compose reads them):

```
MONGO_LOGS_URI=mongodb://mongo_logs:27017/logs
MONGO_META_URI=mongodb://mongo_meta:27017/meta
COLLECTOR_PORT=8080
DASHBOARD_PORT=3000
SAMPLE_SERVICE_PORT=8090
JWT_SECRET=replace-with-at-least-32-character-secret
LOG_BATCH_SIZE=50
LOG_FLUSH_INTERVAL_MS=2000
```

## Quickstart (local)
1. Launch Docker Desktop and open a PowerShell window in the repo root.
2. Run `.\scripts\start-all.ps1` (append `-IncludeSampleService` if you want the demo API). The script wires environment variables, boots MongoDB containers, and opens new shells for the collector and dashboard.
3. Wait for the collector console to print `Started CollectorServiceApplicationKt` and the dashboard console to show `ready - started server on 0.0.0.0:3000`.
4. Browse to http://localhost:3000 and sign in with `admin / admin`.
5. Seed the dashboard demo data with the `docker exec ... mongosh` commands captured in `instructions.txt` to insert slow, broken, and rate-limited logs plus matching alerts.
6. Use `.\scripts\dump-logs.ps1` or `.\scripts\dump-alerts.ps1` to verify the sample payloads, then explore the dashboard filters and alert resolution flow.
7. When finished, run `.\scripts\stop-all.ps1` to close every service and shut down the MongoDB containers.

## Repository Layout

```
tracking-client/     # Kotlin library for API log interception
collector-service/   # Kotlin Spring Boot collector + alerting API
sample-service/      # Demo microservice instrumented with tracking client
dashboard/           # Next.js web application
docker-compose.yml   # End-to-end orchestration
```

## Milestone Status

### Milestone 1 – API Tracking Client
- Auto-configured Spring Boot starter that exposes a `OncePerRequestFilter` for log capture.
- Collects endpoint, method, payload sizes, timestamps, latency, status code, and contextual metadata.
- Non-blocking HTTP sender pushes telemetry to the collector's `/logs` endpoint.
- Fixed-window rate limiter defaults to 100 RPS per service and toggles a `rateLimitHit` flag when exceeded.
- `TrackingMetaContext` helper enables ad-hoc metadata enrichment inside controllers/services.
- Unit tests cover rate limiter window semantics and filter logging behavior.

### Milestone 2 – Collector Service
- Dual Mongo client configuration with dedicated templates, repositories, and no shared state between logs and metadata stores.
- Log ingestion, alert evaluation, dashboard aggregation, issue resolution, and authentication services wired to REST endpoints.
- `/logs` ingestion path is scoped to service principals (tokens with `ROLE_INGESTOR`); the default admin account can read data but cannot push telemetry.
- JWT-based security with default admin bootstrapper and custom filter chain for stateless requests.
- Integration test suite exercising ingestion side effects, dashboard summaries, issue workflows, and login handling using embedded MongoDB (Flapdoodle).

### Milestone 3 – Next.js Dashboard
- Secure React Query-powered dashboard with JWT auth guard, login flow, and middleware-protected routes.
- Time-range aware overview tiles, performance insights, and logs table with filter toggles for errors, latency, and rate-limit hits.
- Alert and issue management widgets with optimistic resolution flows and realtime refresh intervals.
- Tailwind-based UI primitives, nav bar with session handling, and data visualisation (latency bar chart) powered by Recharts.

### Milestone 4 – Non-functional Requirements
- Mongo TTL guardrails on rate-limit and audit collections, enforced at startup through `MongoIndexInitializer`.
- Micrometer + Prometheus instrumentation for ingest counters, rate-limit tracking, and ingestion latency histograms.
- Scheduled Mongo maintenance job that issues compact commands (degrades gracefully for unsupported engines) to keep storage under control.
- Integration coverage for TTL enforcement, metrics, and maintenance execution alongside existing ingestion workflows.

### Sample Service
- `/api/orders` REST API backs demo traffic with tracking-client instrumentation (`TrackingMetaContext` enriches metadata and rate limiter feedback).
- MockWebServer-backed smoke test (`SampleServiceSmokeTest`) validates that each order POST emits a log to the collector endpoint.
- Optional local run via `.\gradlew.bat :sample-service:bootRun` pairs with `scripts\seed-orders.ps1` to replay demo traffic when the collector's ingress role is configured.

## Testing

### Backend (collector-service/)
- `./gradlew :collector-service:test` executes unit and integration suites with embedded MongoDB.
- `./gradlew :collector-service:bootRun` starts the API locally at http://localhost:8080.
- Supply a strong JWT secret via `setx JWT_SECRET ...` or the `start-all.ps1` script before launching.
- `./gradlew dependencyUpdates` helps audit dependencies (requires the Versions plugin already applied).

### Frontend (dashboard/)
- `cd dashboard`
- `npm install` (one time) then `npm run lint` for static analysis.
- `docker compose up -d mongo_logs mongo_meta` to bring MongoDB online before you launch the collector.
- `npx playwright install` the first time to provision browsers, then `npm run test:e2e` for smoke coverage (requires the collector API running locally via `docker compose up` or `./gradlew :collector-service:bootRun`).
- `npm run dev` launches the Next.js development server at http://localhost:3000; use `npm run build && npm run start` for a production check.
- When the dashboard loads, authenticate with the default admin account (`admin` / `admin`). Use the UI to confirm metrics tiles render and rate-limit flags surface when you post logs via the sample service.
- Ensure the collector has a strong JWT secret before launching (`setx JWT_SECRET your-32-char-secret` on Windows PowerShell, or add it to your `.env`).
- CI runs Playwright smoke tests via `.github/workflows/e2e.yml`; replicate locally with `npm run test:e2e` after executing `npx playwright install` once.

## Next Steps
- Restore a dedicated ingestion client with `ROLE_INGESTOR` credentials so scripted traffic can flow through `/logs` instead of direct Mongo inserts.
- Promote the `start-all.ps1` experience into GitHub Actions for one-click preview environments.
- Finalize Kubernetes manifests, alert routing, and observability wiring outlined in `docs/operations-runbook.md` and `docs/observability.md`.

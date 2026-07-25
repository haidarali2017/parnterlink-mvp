# PartnerLink MVP — Merchant Apply Service

Tiny Spring Boot 3.3 + MyBatis + MySQL service for GMO-style merchant onboarding.

**Focus (what this demonstrates):**
- Idempotent `POST /merchants/apply` by `applicationId` (retries never double-issue a merchant number / never double-call MUN)
- `@Transactional` at the use-case service boundary; external MUN call runs **after commit**
- State machine: `APPLIED → SCREENING → APPROVED | REJECTED | TIMEOUT` (illegal transitions → 409)
- Mocked async MUN screening with timeout handling
- Tests for idempotency, illegal transitions, and timeout

No UI — backend only.

## Stack

| Layer | Choice |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| Persistence | MyBatis + MySQL 8 |
| Async | `@Async` + `CompletableFuture.orTimeout` |
| Tests | Spring Boot Test + MockMvc + H2 (`MODE=MySQL`) + Awaitility |

## Quick start

### 1. Start MySQL

```bash
docker compose up -d
```

### 2. Run the app

```bash
./mvnw spring-boot:run
```

Windows: `mvnw.cmd spring-boot:run`

### 3. Apply (first call → 202 SCREENING)

```bash
curl -s -X POST http://localhost:8080/merchants/apply \
  -H "Content-Type: application/json" \
  -d "{\"applicationId\":\"11111111-1111-1111-1111-111111111111\",\"merchantName\":\"Demo Shop\"}"
```

### 4. Retry same applicationId (→ 200, same merchantNumber, no second MUN call)

```bash
curl -s -X POST http://localhost:8080/merchants/apply \
  -H "Content-Type: application/json" \
  -d "{\"applicationId\":\"11111111-1111-1111-1111-111111111111\",\"merchantName\":\"Demo Shop\"}"
```

### 5. Poll status

```bash
curl -s http://localhost:8080/merchants/11111111-1111-1111-1111-111111111111
```

After ~200ms (default mock delay) status becomes `APPROVED`.

### Config knobs (`application.yml`)

```yaml
partnerlink:
  mun:
    delay-ms: 200        # mock MUN latency
    timeout-ms: 2000     # client-side timeout → TIMEOUT status
    default-result: APPROVED
```

## Tests

```bash
./mvnw test
```

| Test | Asserts |
| --- | --- |
| `IdempotencyTest` | Same `applicationId` → one row, one merchant #, MUN called once |
| `StateMachineTest` | Illegal transitions throw `IllegalStatusTransitionException` |
| `TimeoutTest` | When MUN delay > timeout → `TIMEOUT`, never `APPROVED` |
| `ApplicationStatusTest` | Unit-level transition matrix |

## Design notes (interview talking points)

### Where is `@Transactional`?

On `MerchantApplicationService` / `ScreeningOutcomeHandler` (use-case layer) — **not** on the controller or MyBatis mapper.

### How is apply idempotent?

1. Client supplies a stable `applicationId` (UUID).
2. DB has `UNIQUE (application_id)`.
3. First insert wins; duplicate key / existing row → return stored result.
4. Merchant number assigned only when `merchant_number IS NULL`.
5. MUN screening scheduled only on the first successful create (`afterCommit`).

### Why MUN after commit?

Holding a DB transaction open across external I/O risks connection pool exhaustion and partial external side-effects on rollback. Pattern: commit local state (`SCREENING`) → then call MUN → callback updates status in a **new** transaction (`ScreeningOutcomeHandler`).

## Claude Code usage note

**What Claude drafted:** project scaffold (`pom.xml`, package layout), MyBatis mapper XML boilerplate, MockMvc test skeletons, README outline.

**What I rewrote / caught in review (money & state logic):**
- Moved `@Transactional` off any controller path; external MUN only via `TransactionSynchronization.afterCommit`
- Extracted `ScreeningOutcomeHandler` so async callbacks get a real Spring proxy (self-invocation would skip `@Transactional`)
- Idempotency: UNIQUE + duplicate-key race handling + `assignMerchantNumber` only when null + MUN call-count assertion in tests
- State machine guard (`canTransitionTo`) + optimistic `UPDATE ... WHERE status = fromStatus`
- Timeout path: `orTimeout` → `TIMEOUT` status, never silent APPROVED

## Repo

https://github.com/haidarali2017/parnterlink-mvp

# PolicyStrike — Financial Policy Red-Team Engine

> Attack your financial policies before fraudsters do.

PolicyStrike is a Java/Spring Boot application for testing refund policies against synthetic abuse patterns before a policy is activated. It combines Gemini-assisted policy interpretation and adversarial scenario generation with a deterministic Java refund evaluator. Policy versions, simulation runs, and individual refund decisions are persisted in PostgreSQL.

**Core principle: AI proposes. Rules decide.** Gemini does not approve or reject refunds. Financial decisions are made by the Java policy evaluator.

## What it does

1. **Compile a natural-language policy.** Gemini extracts structured limits and controls from a merchant's refund-policy text. The result is a proposal, not an active policy.
2. **Generate adversarial scenarios.** Gemini proposes sequences for threshold splitting, cumulative exposure, and duplicate refund requests.
3. **Validate and execute scenarios.** The application validates generated scenarios and sends their refund steps through the deterministic Java evaluator.
4. **Measure simulation outcomes.** A policy-calibrated synthetic benchmark evaluates 100 customer profiles and 1,000 refund requests, reporting approvals, manual reviews, blocks, approved exposure, false positives, missed attacks, and per-attack detection counts.
5. **Version and approve policies.** New versions start as `PENDING_APPROVAL`. A reviewer can compare a saved candidate with a saved baseline, then approve or reject it. Approving a candidate activates it and marks the previous active version `SUPERSEDED`.
6. **Persist audit history.** Simulation runs and individual refund decisions are stored in PostgreSQL and can be retrieved through paginated API endpoints.
7. **Explain replay findings.** Gemini can explain deterministic replay results. Its explanation is advisory; replay output remains the source of truth.

## Technology stack

- Java 21 language target
- Spring Boot 4.2.0, Spring Web MVC
- Spring Data JPA / Hibernate
- PostgreSQL
- Google GenAI Java SDK
- JUnit 5, Mockito, Maven
- Springdoc OpenAPI / Swagger UI

## Architecture

```text
Swagger UI / REST clients
          |
          v
     API controllers
          |
          v
      Service layer -------------------- Gemini Gateway
          |                                    |
          v                                    v
  Deterministic Java engine          Policy interpretation,
          |                           scenario suggestions,
          |                           vulnerability explanations
          v                                    |
  Simulation + replay <------------------------+
          |
          v
  PostgreSQL persistence
  - policy versions
  - simulation runs
  - attack results
  - refund audit records
```

Gemini returns structured proposals and explanations. Java validates policy inputs, evaluates refund requests, computes exposure, and produces the final decision. Model output is never executable policy code.

## Requirements

- JDK 21 or later (the project compiles with `--release 21`)
- Maven 3.9+
- Docker Desktop or a local PostgreSQL 17 instance
- A Google AI Studio API key for Gemini-backed endpoints

## Local setup

### 1. Configure PostgreSQL

For a local Docker database:

```bash
docker run --name policystrike-db -e POSTGRES_DB=policystrike -e POSTGRES_USER=policystrike -e POSTGRES_PASSWORD=policystrike -p 5432:5432 -d postgres:17
```

For subsequent runs:

```bash
docker start policystrike-db
```

The password above is a local-development example. Override it outside a disposable local environment.

### 2. Configure environment variables

Set these in the same terminal that starts Spring Boot:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/policystrike"
export DB_USERNAME="policystrike"
export DB_PASSWORD="your-local-db-password"
export GOOGLE_API_KEY="your-google-ai-studio-key"
export GEMINI_MODEL="gemini-3.5-flash-lite"
```

`GEMINI_MODEL` defaults to `gemini-3.5-flash-lite` in the current configuration. The model was tested successfully with the project's Google AI API key during development. Model availability and quotas are controlled by Google and can change.

Do not commit API keys, passwords, `.env` files, or other credentials. Never paste secrets into issue reports or source control.

### 3. Build and test

From the repository root:

```bash
mvn clean test
```

The verified test run on November 24, 2026 completed with **29 tests, 0 failures, 0 errors, and 0 skipped**.

### 4. Start the application

```bash
mvn spring-boot:run
```

The application listens on port `8080` by default.

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Health endpoint: http://localhost:8080/api/v1/system/health

On startup, the application initializes the default `PS-001` policy only when that policy does not already exist. The default initial policy is version 1 with a ₹7,500 individual automatic-refund limit, ₹20,000 customer cumulative limit, 24-hour threshold window, 30-day cumulative window, and duplicate protection enabled.

## API overview

All endpoints are rooted at `/api/v1`.

| Capability | Method and endpoint | Purpose |
|---|---|---|
| Health | `GET /system/health` | Application health check |
| Compile policy | `POST /ai/policies/compile` | Convert natural-language policy text into a structured proposal |
| Generate attacks | `POST /ai/policies/{policyId}/scenarios/generate` | Generate adversarial scenarios for the active policy |
| Execute generated attacks | `POST /ai/policies/{policyId}/scenarios/execute` | Validate and evaluate a generated scenario response using Java rules; persist results |
| Explain vulnerabilities | `POST /ai/vulnerabilities/explain` | Generate an explanation of deterministic replay results |
| List policy versions | `GET /policies/{policyId}/versions` | Retrieve policy version history and lifecycle metadata |
| Create policy version | `POST /policies/{policyId}/versions` | Create a new `PENDING_APPROVAL` proposal |
| Approve version | `POST /policies/{policyId}/versions/{version}/approve` | Activate a pending version and supersede the previous active version |
| Reject version | `POST /policies/{policyId}/versions/{version}/reject` | Reject a pending version with a reason |
| Replay latest pair | `POST /replays/run?policyId=PS-001` | Compare the two newest non-rejected saved versions |
| Replay explicit pair | `POST /replays/{policyId}/run?baselineVersion=1&candidateVersion=2` | Compare specified saved versions |
| Run benchmark | `POST /simulations/run?policyId=PS-001` | Run the 100-customer / 1,000-request synthetic benchmark against the active version |
| List runs | `GET /simulations` | List saved simulation runs |
| Get run | `GET /simulations/{runId}` | Retrieve a saved run |
| Get audits | `GET /simulations/{runId}/audits?page=0&size=20` | Retrieve paginated refund audit records |

For exact request and response schemas, use Swagger UI. The scenario-execution endpoint accepts the complete JSON response from the scenario-generation endpoint; do not substitute Swagger's placeholder example values.

## Policy lifecycle

```text
Create proposal
      |
      v
PENDING_APPROVAL
      |
      +----> Replay saved baseline vs candidate
      |
      +----> Approve -----------------> ACTIVE
      |                                  |
      |                                  +--> previous active version: SUPERSEDED
      |
      +----> Reject ------------------> REJECTED
                                         previous active version remains active
```

Normal simulations, scenario generation, and generated-scenario execution load the active policy version. A pending proposal does not affect live evaluation. Replay can load a pending candidate explicitly for comparison.

The current reviewer fields (`approvedBy` and `rejectedBy`) are request-supplied audit labels. They are not authenticated identities.

## Attack patterns

- **Threshold splitting:** Multiple individually compliant requests are submitted within the configured monitoring window, with their combined value crossing the threshold control.
- **Cumulative exposure:** Requests are spaced so the threshold-splitting rule does not trigger, but their aggregate value crosses the customer's cumulative limit.
- **Duplicate request:** The same original transaction reference is submitted more than once; duplicate protection should block repeats.

The generated-scenario execution endpoint isolates each scenario's synthetic customer state, preserves transaction references, orders steps chronologically, and runs each step through the same deterministic evaluator used by the benchmark.

## Verified demonstration snapshot

The following is the local synthetic benchmark result observed with `PS-001` version 2 active on November 24, 2026:

| Metric | Observed result |
|---|---:|
| Customer profiles | 100 |
| Refund requests | 1,000 |
| Automatically approved | 970 |
| Manual review | 0 |
| Blocked | 30 |
| Approved exposure | INR 286,600 |
| False-positive blocks | 0 |
| Missed attacks | 0 |
| Threshold-splitting detections | 10 / 10 |
| Cumulative-exposure detections | 10 / 10 |
| Duplicate-request detections | 10 / 10 |

These figures describe the deterministic synthetic benchmark, not real-world production traffic or fraud-detection accuracy. The benchmark dataset is calibrated to the policy under test so benign requests remain within its configured limits and injected requests exercise the intended control.

## Tests

The verified Maven test suite contains six test classes:

- `RefundPolicyEvaluatorTest`
- `PolicyStrikeSimulatorTest`
- `SimulationExecutionTest`
- `PolicyVersionReplayServiceTest`
- `PolicyVersionServiceTest`
- `GeneratedScenarioExecutionServiceTest`

Run all tests with `mvn clean test`.

## Known limitations and production hardening

This project is an interview/portfolio MVP, not a production financial decisioning service.

- **Authentication and authorization:** API endpoints are not protected by a full authentication/authorization layer. Reviewer identity is supplied in the request body. Add Spring Security, authenticated principals, reviewer roles, and authorization checks before deployment.
- **Database migrations:** Hibernate currently uses `spring.jpa.hibernate.ddl-auto=update` for local development. Use versioned database migrations (for example, Flyway) and controlled schema rollout for production.
- **Database credentials:** `application.properties` has local fallback values. Set strong secrets through environment variables or a secret manager in deployed environments; remove development fallbacks from production configuration.
- **Gemini availability:** Gemini requests can fail due to transient model capacity, network, permission, or quota issues. Add/verify bounded retries, timeouts, structured upstream error mapping, and operational logging before relying on AI endpoints in production.
- **Human review:** The approval flow demonstrates policy governance but does not provide authenticated separation of duties or a signed approval record.
- **Synthetic benchmark:** Zero false positives and zero missed attacks are results for the included deterministic test dataset only. They do not establish real-world accuracy.
- **Operational controls:** Add rate limiting, request-size limits, observability, backup/restore procedures, and deployment-specific security controls before exposing the API publicly.

## Repository hygiene

The repository `.gitignore` should exclude at least:

```gitignore
target/
.idea/
.vscode/
*.iml
.DS_Store
.env*
!.env.example
*.log
```

Ignoring a file does not remove it from Git history if it was committed earlier. Before publishing the repository, inspect tracked files and history for secrets. If a real credential was ever committed, rotate it; removing the file from the latest commit alone is not sufficient.

## License

Add a license file before distributing this project publicly. No license is declared by this README.

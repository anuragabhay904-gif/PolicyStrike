# RefundShield — Revenue Leak Recovery Agent

> An AI-powered, autonomous financial revenue leak detection and recovery platform for merchants. Built as a high-velocity, 1-day hackathon MVP.

---

## 📌 Problem Statement

Merchants lose substantial recoverable revenue every day due to:
1. **Failed Payment Transactions**: Payment gateway timeouts, bank authentication drops, or network glitches where a customer had high purchase intent.
2. **High-Value Abandoned Checkouts**: Large shopping baskets abandoned right before or during authorization.
3. **Dormant Repeat Customers**: High-LTV repeat buyers who suddenly stop re-ordering past their typical purchase cycle.

Most merchants lack automated systems to intercept these leaks in real time, analyze the root cause, and re-engage customers with verified recovery payment links before the revenue is permanently lost.

---

## 💡 Solution

**RefundShield** couples **deterministic financial rule engines** with **autonomous Gemini AI reasoning**:
- **Deterministic Backend Engine**: Identifies revenue leaks, calculates a normalized **Recovery Score (0–100)**, validates amounts, enforces state machines, and interfaces with the **Razorpay API**.
- **Autonomous AI Layer**: Synthesizes customer purchase history and checkout drop-off patterns to generate human-readable explanations, operational recommendations, and personalized customer outreach copy.
- **Merchant-in-the-Loop Approval**: The LLM *never* independently authorizes charges or alters amounts. The merchant reviews the opportunity, inspects the reasoning, and explicitly approves the action.
- **Instant Razorpay Payment Links**: Upon approval, generates a secure Razorpay Test Mode payment link and records every step in an **immutable audit trail**.

---

## 🏗 Architecture & Core Principles

```
   ┌────────────────────────────────────────────────────────┐
   │                    Merchant Dashboard                  │
   │               (React + Vite + Tailwind CSS)            │
   └───────────────────────────┬────────────────────────────┘
                               │ REST APIs (/api/*)
                               ▼
   ┌────────────────────────────────────────────────────────┐
   │                 Spring Boot 3 Monolith                 │
   │                                                        │
   │  ┌───────────────────────┐  ┌───────────────────────┐  │
   │  │ Revenue Leak Detector │  │ Recovery Score Engine │  │
   │  │  - Failed Payments    │  │  - Recency Weight     │  │
   │  │  - Abandoned Carts    │  │  - Spend / Frequency  │  │
   │  │  - Inactive Buyers    │  │  - Failure Multiplier │  │
   │  └───────────┬───────────┘  └───────────┬───────────┘  │
   │              │                          │              │
   │              ▼                          ▼              │
   │  ┌──────────────────────────────────────────────────┐  │
   │  │          Opportunity & Validation Engine         │  │
   │  └───────────────┬───────────────────┬──────────────┘  │
   │                  │                   │                 │
   └──────────────────┼───────────────────┼─────────────────┘
                      │                   │
                      ▼                   ▼
          ┌───────────────────────┐   ┌────────────────────────┐
          │     Google Gemini     │   │     Razorpay API       │
          │   Contextual Analysis │   │   (Test Mode Links     │
          │  & Outreach Messaging │   │   or Realistic Demo)   │
          └───────────────────────┘   └────────────────────────┘
```

### Core Architecture Rules
1. **Deterministic Authority**: Financial logic, detection rules, recovery scoring, eligibility checks, and deduplication are strictly governed by Java Spring Boot backend services.
2. **AI Guardrails**: Gemini is restricted via structured JSON schemas and strict prompting. It cannot modify amounts, charge cards, or hallucinate records.
3. **Graceful Fallback**: If external APIs (Gemini or Razorpay) are unreachable or unconfigured, the application gracefully degrades to deterministic fallbacks and simulated demo links.
4. **Auditability**: Every detection, AI inference, merchant approval, link creation, and gateway failure writes directly to an immutable audit event stream.

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| **Frontend** | React 18, Vite 5, JavaScript, Tailwind CSS, Lucide Icons, Recharts, Axios |
| **Backend** | Java 21/24, Spring Boot 3.3.5, Spring Web, Spring Data JPA, Hibernate, Jackson |
| **Database** | H2 (Local In-Memory / Zero Setup) & PostgreSQL Dialect (Production-ready) |
| **AI** | Google Gemini API (`gemini-2.0-flash`) |
| **Payment Gateway** | Razorpay Test Mode API (`/v1/payment_links`) with DEMO simulator mode |

---

## 📂 Project Structure

```
.
├── backend/
│   ├── src/main/java/com/refundshield/recoveryagent/
│   │   ├── config/             # Web CORS, RestTemplate configuration
│   │   ├── controller/         # REST Controllers (Dashboard, Opportunities, Audit, Demo)
│   │   ├── dto/                # Request/Response payloads (OpportunityDTO, AiAnalysisDTO, etc.)
│   │   ├── entity/             # JPA Entities (Customer, Transaction, Opportunity, RecoveryAction, AuditEvent)
│   │   ├── exception/          # GlobalExceptionHandler and custom exceptions
│   │   ├── repository/         # Spring Data JPA Repositories
│   │   ├── service/            # Leak detection, scoring calculator, Gemini, Razorpay, Audit services
│   │   └── RevenueLeakRecoveryAgentApplication.java
│   ├── src/main/resources/
│   │   ├── application.properties        # Default H2 zero-setup config
│   │   └── application-prod.properties   # Production PostgreSQL config
│   ├── pom.xml
│   └── .env.example
├── frontend/
│   ├── src/
│   │   ├── components/         # Navbar, Sidebar, Toast notification system
│   │   ├── pages/              # Dashboard, Opportunities, Opportunity Details, Ledger, Audit
│   │   ├── services/           # Axios API client
│   │   ├── App.jsx
│   │   ├── main.jsx
│   │   └── index.css
│   ├── package.json
│   ├── tailwind.config.js
│   └── vite.config.js
├── README.md
└── .gitignore
```

---

## ⚙️ Environment Configuration

### Backend (`backend/.env` or system environment variables)
Copy `backend/.env.example` to `backend/.env`:

```env
# Payment Gateway (DEMO or RAZORPAY)
PAYMENT_MODE=DEMO

# Razorpay Test Credentials (Optional if PAYMENT_MODE=DEMO)
RAZORPAY_KEY_ID=rzp_test_YourKeyId
RAZORPAY_KEY_SECRET=YourKeySecret

# Google Gemini API Key (Optional: deterministic fallback used if absent)
GEMINI_API_KEY=AIzaSyYourGeminiApiKey
GEMINI_MODEL=gemini-2.0-flash

# Port Configuration
SERVER_PORT=8080
```

---

## 🚀 Running the Project

### Prerequisites
- **Java 17, 21, or 24**
- **Maven 3.8+**
- **Node.js 18+ & npm**

### 1. Run Backend
```bash
cd backend
mvn clean compile
mvn test                   # Runs comprehensive unit and integration test suite
mvn spring-boot:run        # Starts backend on http://localhost:8080
```
> *On startup, the backend automatically seeds 10 customers, 26 transactions, and initializes the first batch of detected revenue leaks.*

### 2. Run Frontend
```bash
cd frontend
npm install
npm run dev                # Starts Vite dev server on http://localhost:5173
```
Open **[http://localhost:5173](http://localhost:5173)** in your browser.

---

## 🎬 2-Minute Hackathon Demo Script

Follow this step-by-step walkthrough to present the MVP to judges:

1. **Open the Dashboard**:
   - Point out the **₹1,09,690.00** total revenue captured vs **₹41,700.24** potential recoverable revenue.
   - Show the visual comparison chart between captured settlements and intercepted leaks.

2. **Simulate a Fresh Revenue Leak**:
   - Click the **"Simulate Revenue Leak"** button in the top navbar.
   - A live transaction failure is generated (e.g. ₹5,499.00).
   - The system automatically identifies the leak and routes you to the **Opportunity Detail Page**.

3. **Inspect Deterministic Scoring**:
   - Point out the **Recovery Score (e.g. 86/100 or 100/100)** computed deterministically based on buyer purchase history, transaction channel, and basket value.

4. **Generate AI Analysis**:
   - Click **"Generate AI Analysis"**.
   - Gemini evaluates the drop-off pattern and produces:
     - **Why This Revenue is at Risk**
     - **Recommended Operational Action**
     - **Personalized Customer Outreach Copy** (with 1-click clipboard copy).

5. **Merchant Approval & Razorpay Test Payment Link**:
   - Click **"Approve Recovery"**.
   - The backend validates the opportunity state, creates a `RecoveryAction`, and generates a verified Razorpay Test Mode link (e.g. `https://rzp.io/i/...`).
   - Click **"Test Link"** to preview the payment link.

6. **Verify the Audit Trail**:
   - Navigate to the **Audit Trail** tab to show immutable timestamps and actor badges (`SYSTEM`, `AI_AGENT`, `MERCHANT`).

7. **Test Failure Scenario (Controlled Resilience Demo)**:
   - In the top navbar, toggle **"Failure Simulation: ON"**.
   - Attempt to approve another recovery opportunity.
   - The system intercepts the 503 gateway failure, flags the opportunity as `FAILED`, presents a clear error toast without crashing, and logs the failure to the audit trail.
   - Toggle it back to **"OFF"** to restore normal operation.

---

## 📡 REST API Reference

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/health` | Service health, active payment mode, and AI model |
| `GET` | `/api/dashboard` | Aggregated KPIs, pipeline breakdown, recent activities |
| `GET` | `/api/opportunities` | List opportunities (optional `?status=NEW`) |
| `GET` | `/api/opportunities/{id}` | Single opportunity details with customer & transaction context |
| `POST` | `/api/opportunities/{id}/analyze` | Trigger Gemini AI reasoning & outreach generation |
| `POST` | `/api/opportunities/{id}/approve` | Validate approval, call Razorpay, create payment link |
| `POST` | `/api/opportunities/{id}/dismiss` | Dismiss an opportunity |
| `GET` | `/api/audit` | View full audit trail (optional `?opportunityId=...`) |
| `GET` | `/api/transactions` | Merchant transaction ledger (optional `?status=...`) |
| `GET` | `/api/customers` | Merchant customer list |
| `POST` | `/api/demo/seed` | Reset & seed 10 customers and 26 transactions |
| `POST` | `/api/demo/simulate-leak` | Generate a new simulated failed transaction & opportunity |
| `POST` | `/api/demo/simulate-failure` | Toggle controlled gateway failure mode (`?enabled=true/false`) |

---

## 🚢 Production Deployment

### Frontend (Vercel)
1. Push repository to GitHub.
2. Import root repo into Vercel. Set **Root Directory** to `frontend`.
3. Set Environment Variable: `VITE_API_URL=https://your-backend.railway.app`
4. Deploy.

### Backend (Railway / Render / Docker)
1. In Railway or Render, create a new Web Service pointing to `backend`.
2. Set Environment Variables:
   - `PAYMENT_MODE=RAZORPAY`
   - `RAZORPAY_KEY_ID=...`
   - `RAZORPAY_KEY_SECRET=...`
   - `GEMINI_API_KEY=...`
   - `SPRING_PROFILES_ACTIVE=prod`
   - `DATABASE_URL=jdbc:postgresql://...`
   - `DATABASE_USERNAME=...`
   - `DATABASE_PASSWORD=...`
3. Deploy.

---

## 🔒 Security & Safe Harbor
- Razorpay API secrets and Gemini keys are injected strictly via environment variables.
- All approval amounts are validated on the backend and cannot be tampered with from the client.
- The AI has no permission to initiate fund movement or sign financial orders.

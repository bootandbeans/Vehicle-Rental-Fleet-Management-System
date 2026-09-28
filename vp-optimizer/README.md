# Product & Volume Point Optimization System

Given a **subset of products chosen by the user**, a discount, a GST rate, a **target Volume Point (VP)**
value and a tolerance, the system returns the **best 2–3 integer quantity combinations** that reach the
target VP window while keeping the **final payable amount** as low as possible.

The optimizer is the product here, not a CRUD screen: prices are computed with `BigDecimal` in exactly
one place, the combination search is a bounded dynamic program over the VP axis, results are ranked
lexicographically (never with an arbitrary weighted score) and every returned solution is explained from
its own numbers. Results are stored with a full product and pricing snapshot, so an old run stays
reproducible after catalogue prices change.

```
React + TypeScript + Vite + MUI  ──HTTP/JSON──▶  Spring Boot 3 (Java 21)  ──JPA──▶  PostgreSQL 16
        frontend/                                 backend/                          Flyway migrations
```

---

## Table of contents

1. [Feature overview](#1-feature-overview)
2. [Architecture](#2-architecture)
3. [Quick start](#3-quick-start)
4. [Configuration](#4-configuration)
5. [Domain rules: pricing, VP and tolerance](#5-domain-rules-pricing-vp-and-tolerance)
6. [**The optimization engine (algorithm)**](#6-the-optimization-engine-algorithm)
7. [REST API](#7-rest-api)
8. [Persistence, history and snapshots](#8-persistence-history-and-snapshots)
9. [Frontend](#9-frontend)
10. [Testing](#10-testing)
11. [Error codes](#11-error-codes)
12. [Deliberately out of scope (extensibility)](#12-deliberately-out-of-scope-extensibility)
13. [Project layout](#13-project-layout)
14. [Deployment](#14-deployment)

---

## 1. Feature overview

| Area | What the system does |
| --- | --- |
| Product catalogue | Full CRUD over products and categories: MRP, VP per unit, optional min/max quantity, soft delete (`active`) so history keeps working, SKU uniqueness, search, filter, sort, pagination |
| Explicit selection | The optimization request always carries the selected product ids. Products that were not selected can never appear in a result — not even cheaper ones |
| Selection states | `ALLOWED` (may be used) and `REQUIRED` (must be used, honours `minQuantity`); `EXCLUDED` is the absence of a selection. The model already carries the enum, so a future `REQUIRED`-only workflow needs no refactor |
| Pricing | `discountedPrice = mrp × (1 − discount/100)`, `gstAmount = discountedPrice × gst/100`, `finalPrice = discountedPrice + gstAmount`, all with `BigDecimal`, scale 2, `HALF_UP`, calculated once in `StandardPricingCalculator` |
| Optimization | Bounded dynamic program over the VP axis, cheapest combination per reachable VP total, deterministic ranking, up to `resultLimit` solutions (default 3) |
| Explanations | Every solution carries a sentence generated from its actual values, plus explicit `Exact target` / `Within range` / `Outside range` indicators |
| No solution | An explicit "No valid combination found within the requested VP range" message plus the closest alternatives, clearly labelled as outside the requested range |
| History | Every run is persisted with the products, effective quantity ranges and pricing of that moment, plus the returned combinations — reopening a session shows the original numbers |
| API | REST + OpenAPI/Swagger, uniform `{code, message}` errors from a single `@RestControllerAdvice` |

---

## 2. Architecture

```
vp-optimizer/
├── backend/                        Spring Boot 3.5 / Java 21 / Maven
│   └── src/main/java/com/example/vpoptimizer/
│       ├── optimization/           ← the engine: pure Java, no Spring, no JPA, no HTTP
│       │   ├── model/              ProductOption, PricingContext, VpRange, Solution, OptimizationLimits …
│       │   ├── calculator/         Money, PricingCalculator, StandardPricingCalculator, CostPerVpCalculator
│       │   ├── engine/             OptimizationEngine (interface), IntegerOptimizationEngine, SolutionAssembler
│       │   └── ranking/            SolutionRanker, SolutionExplainer
│       ├── service/                use cases / transaction boundaries (no business logic in controllers)
│       ├── controller/             thin HTTP layer, validation happens on the DTOs
│       ├── repository/             Spring Data JPA
│       ├── entity/                 JPA entities (schema owned by Flyway)
│       ├── dto/ + mapper/          API contracts and mapping
│       ├── validation/             custom bean validation (`@ValidOptimizationRequest`, `@ValidQuantityRange`)
│       ├── exception/              ApiErrorCode, BusinessException, GlobalExceptionHandler
│       └── config/                 OptimizationProperties, CORS, OpenAPI, engine beans
└── frontend/                       React 19 + TypeScript + Vite + MUI
    └── src/
        ├── engine/                 TypeScript port of the *same* engine (offline/demo mode only)
        ├── services/               httpApi (REST) and demoApi (in-browser), one interface
        ├── pages/, components/     UI
        └── context/, hooks/        app settings + tiny data-fetching helpers
```

Rules the code sticks to:

* the engine depends on nothing but the JDK — controllers, DTOs and JPA entities never leak into it;
* no pricing formula exists twice inside the backend (the frontend only *formats* numbers it receives);
* no business logic in controllers and no optimization work inside SQL — the selected rows are loaded
  into memory and the search runs in Java;
* the frontend always calls `/api` **relatively**; Vite (dev) and nginx (docker) proxy it, so the browser
  never hard-codes a backend host.

---

## 3. Quick start

### Docker Compose (everything)

```bash
cd vp-optimizer
cp .env.example .env          # optional, defaults work
docker compose up --build
```

| Service | URL |
| --- | --- |
| UI | http://localhost:3000 |
| REST API | http://localhost:8080/api/products |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/api/health |

PostgreSQL starts empty, Flyway creates the schema and seeds six categories and seven demo products.

### Local development

```bash
# 1. PostgreSQL (any instance works)
docker run --rm -p 5432:5432 -e POSTGRES_DB=vpoptimizer \
  -e POSTGRES_USER=vpoptimizer -e POSTGRES_PASSWORD=vpoptimizer postgres:16-alpine

# 2. Backend  ->  http://localhost:8080
cd vp-optimizer/backend
mvn spring-boot:run             # needs JDK 21 + Maven 3.9+

# 3. Frontend ->  http://localhost:5173 (proxies /api to localhost:8080)
cd vp-optimizer/frontend
npm install
npm run dev
```

`npm run dev` uses `VITE_API_PROXY_TARGET` (default `http://localhost:8080`). If no backend answers,
the UI switches to **demo mode** and runs the TypeScript port of the engine in the browser, so the whole
flow (including history) is explorable without Java or PostgreSQL — the header shows which mode is
active and the data source can be pinned in *Settings*.

### Deployment

```bash
# frontend only (demo mode, ~3 minutes)        -> Vercel / Netlify, see DEPLOYMENT.md
# full stack                                    -> Neon + Render (Blueprint) + Vercel or Netlify
```

* `frontend/vercel.json` and the repository-root `netlify.toml` make both hosts build the SPA without
  any dashboard configuration (SPA rewrites included).
* `render.yaml` deploys the API container plus PostgreSQL in one Blueprint; the image translates the
  platform's `DATABASE_URL` into JDBC settings at start-up (`backend/entrypoint.sh`).
* Vercel and Netlify cannot run the Spring Boot process — the backend belongs on a container host.
  Vite bakes the API URL in at build time via `VITE_API_BASE_URL`.

Step-by-step instructions, environment variables, verification commands and a troubleshooting table:
**[DEPLOYMENT.md](DEPLOYMENT.md)**.

---

## 4. Configuration

`backend/src/main/resources/application.yml` (all values are environment overridable):

| Key | Default | Meaning |
| --- | --- | --- |
| `vp-optimizer.optimization.default-result-limit` | `3` | Solutions returned when the request does not specify a limit |
| `vp-optimizer.optimization.maximum-result-limit` | `10` | Hard ceiling for a caller supplied `resultLimit` |
| `vp-optimizer.optimization.max-selected-products` | `25` | Products allowed in a single run |
| `vp-optimizer.optimization.default-max-quantity-per-product` | `10` | Quantity cap for products without `maxQuantity` |
| `vp-optimizer.optimization.max-vp-capacity` | `20000` | Absolute ceiling of the DP VP axis (see [§6.7](#67-limits-and-complexity)) |
| `spring.datasource.*` | `DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD` | Database connection |
| `vp-optimizer.cors.allowed-origins` | `localhost:5173/4173/3000` | Browser origins allowed to call the API |
| `vp-optimizer.cors.allowed-origin-patterns` | `http://localhost:*`, `http://127.0.0.1:*`, `https://*.e2b.app` | Pattern based origins (proxied preview hosts) |

---

## 5. Domain rules: pricing, VP and tolerance

**Money policy** (`optimization/calculator/Money.java`)

* `BigDecimal` only — never `double`/`float`. Amounts use scale `2`, rates scale `4`, cost-per-VP scale `4`;
* rounding is `HALF_UP` **after every step**, not only at the end;
* internally the engine converts money to integer **minor units (paise)** so the dynamic program adds
  exact integers instead of tolerating floating point drift.

**Unit pricing** (`StandardPricingCalculator` — the only place these formulas exist)

```
discountedPrice = mrp × (1 − discountPercent / 100)
gstAmount       = discountedPrice × gstPercent / 100
finalPrice      = discountedPrice + gstAmount            // final payable per unit
```

Specification example: MRP `2000`, discount `20%`, GST `18%` → `1600.00`, `288.00`, **`1888.00`**.
GST `0%` and discount `0%` are fully supported; a discount of `100%` yields a payable of `0.00`.

**Combination totals**

```
TotalVP   = Σ (volumePoint_i × quantity_i)        // integer VP
TotalCost = Σ (finalPrice_i  × quantity_i)        // final payable
```

**Tolerance normalization** (`VpRange.of`)

| Type | Delta | Example |
| --- | --- | --- |
| `PERCENTAGE` | `round(target × tolerance / 100)` (HALF_UP) | 500 VP ± 10% → `450 … 550` |
| `ABSOLUTE` | `round(tolerance)` | 500 VP ± 25 → `475 … 525` |

`minimumAllowedVP = max(0, target − delta)`, `maximumAllowedVP = target + delta`. A percentage above
`100` is rejected; a target of `0` VP is handled explicitly (the cheapest valid answer is buying nothing).

**Selection states** (`SelectionType`)

* `REQUIRED` — must appear in every returned combination; its effective minimum quantity is
  `max(1, minQuantity)`;
* `ALLOWED` — may be used, minimum quantity `0`;
* excluded — simply not part of the request, therefore invisible to the engine.

---

## 6. The optimization engine (algorithm)

### 6.1 Problem statement

Given selected products `i = 1…n`, each with unit price `c_i` (final payable per unit), VP `v_i`, and an
integer quantity range `[min_i, max_i]`, minimise the total cost

```
minimise   Σ c_i · x_i          subject to      minimumVP ≤ Σ v_i · x_i ≤ maximumVP
```

with `x_i ∈ [min_i, max_i] ⊂ ℤ`, `x_i = 0` allowed for `ALLOWED` products, `x_i ≥ 1` for `REQUIRED`
products. Return the best **N** distinct combinations, not just one.

This is a bounded integer knapsack over the VP axis with an interval constraint instead of a single
capacity. Trivial brute force is `Π (max_i − min_i + 1)` combinations: with 25 products and 10 units each
that is `11²⁵`, so a naive recursion is not an option.

### 6.2 Why a dynamic program (and not an ILP solver)

* **Exact and deterministic.** The DP keeps the cheapest combination for *every* reachable VP total, so
  "cheapest at exactly 500 VP" and "cheapest inside 450…550" are both read off the same table — no solver
  tolerance, no branch-and-bound heuristics, no tie ambiguity.
* **Multiple solutions for free.** Because every reachable VP level is priced, returning the top 3 for a
  *range* (not a single target) is a selection problem, not a re-optimization problem.
* **Zero runtime dependencies.** The engine stays pure Java (`java.math` only), so it is trivially unit
  testable and independent of Spring, JPA and any solver library.
* **Explainable.** Every returned combination can be reconstructed from the recorded choices and explained
  from real numbers, which is a hard requirement here.

An ILP/MILP formulation (or a min-cost flow) remains a legitimate future extension behind the
`OptimizationEngine` interface — the service depends on the interface, not on the implementation.

### 6.3 The algorithm

`optimization/engine/IntegerOptimizationEngine.java`

Let `C` be the DP capacity in VP (see [§6.5](#65-capacity-and-safety-limits)) and let `k_i = max_i − min_i`
be the number of *extra* units a product may contribute.

```
 1. bound        for every product:  min_i, max_i, k_i, unit price in paise, v_i
 2. pre-allocate REQUIRED products:          mandatoryVP  = Σ (min_i · v_i)
                                             mandatoryCost = Σ (min_i · c_i)
 3. DP table     reach[level] = (cost, uniqueProducts, quantity) kept as the lexicographic minimum
                 reach[0] = (mandatoryCost, |REQUIRED products|, Σ min_i)
 4. for each product i (ascending id):
        for each reachable level L:
            for q in 0 … k_i such that L + q·v_i ≤ C:
                relax (cost + q·c_i, unique + (q>0), quantity + q) into level L + q·v_i
 5. reconstruct  walk the recorded choices table backwards -> quantities x_i = min_i + q_i
 6. assemble     drop x_i = 0 lines, compute unit prices, totals, canonical key, explanation
 7. rank         in-range solutions first, then alternatives (see §6.6)
```

Key implementation details:

* **Cost in paise.** The DP compares integer minor units, so the "cheapest" decision is exact and cannot
  be flipped by floating point rounding. Amounts are converted back to `BigDecimal`/scale-2 only when a
  solution is assembled.
* **Lexicographic state.** Each reachable level stores `(cost, distinct products, units)` — the
  first key is the real objective, the other two are deterministic tie-breakers that also make
  "fewer distinct products" prefer the simpler purchase.
* **One candidate per reachable VP total.** For each level the DP keeps the best combination, which is
  exactly the Pareto-relevant candidate: any other combination with the same VP and a higher cost can
  never win the ranking.
* **`min_i` as an offset.** Required minimum quantities are pre-allocated instead of being enforced
  inside the transitions, which keeps every DP state feasible by construction (`REQUIRED` products can
  therefore never be dropped).
* **Zero-VP products.** A product with `volumePoint = 0` only adds cost, so only `q = 0` is ever
  considered for it; if the target itself is `0` VP, the empty combination wins with a payable of
  `0.00` and `costPerVp = null` (no division by zero).
* **Products are processed in ascending id order**, and the DP compares states lexicographically, so the
  result does not depend on the order of the request array.

### 6.4 Worked example (specification scenario)

Products `A(2000, VP 50)`, `B(3000, VP 100)`, `C(1500, VP 40)`, `D(5000, VP 200)`; **A, B and D are
selected, C is not**; discount `20%`, GST `18%`, target `500 VP`, tolerance `10%` → accepted range
`450 … 550 VP`.

Unit prices after pricing: `A = 1888.00`, `B = 2832.00`, `D = 4720.00`.

| Rank | Combination | Total VP | VP diff | Final payable | Cost / VP | Why this rank |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `B × 1 + D × 2` | 500 | 0 | **₹ 12,272.00** | 24.5440 | exact target, cheapest way to hit it |
| 2 | `A × 1 + D × 2` | 450 | 50 | ₹ 11,328.00 | 25.1733 | in range; equal distance as rank 3 but cheaper |
| 3 | `A × 1 + B × 1 + D × 2` | 550 | 50 | ₹ 14,160.00 | 25.7455 | in range; same distance, higher cost |

Product `C` never appears: it is not in the request, so it is not in the DP. Note that rank 2 is
*cheaper* than rank 1 — a strictly cheaper combination is correctly ranked lower because it is further
from the target. That is the point of the ranking order.

### 6.5 Capacity and safety limits

The DP axis must be bounded, otherwise an unrealistic target would allocate gigabytes:

```
achievableExtraVP = Σ k_i · v_i
headroom          = min(largest single-unit VP, maxVpCapacity)
C = max(0, min(achievableExtraVP, max(0, maximumAllowedVP − mandatoryVP) + headroom,
                               maxVpCapacity + headroom))
```

* `maxVpCapacity` (default `20000`) caps the axis, and a request whose accepted window exceeds it is
  rejected with `ENGINE_LIMIT_EXCEEDED` and a message telling the caller to lower the target or tolerance;
* the extra `headroom` step guarantees the *closest alternatives* on the upper side are still reachable
  while keeping the table small;
* products without a `maxQuantity` are treated as unbounded but capped by
  `defaultMaxQuantityPerProduct` (default `10`), so work always terminates;
* a run may use at most `maxSelectedProducts` (default `25`) products — at that size the table stays in
  the low tens of megabytes at most, and typical catalogues are far smaller.

### 6.6 Ranking, deduplication and alternatives

`optimization/ranking/SolutionRanker.java` — pure lexicographic comparison, **no weighted score**:

1. combinations **inside** the accepted VP window first;
2. smaller `|totalVP − targetVP|`;
3. smaller **final payable amount**;
4. fewer distinct products;
5. fewer units in total;
6. canonical key (deterministic, stable final tie-break).

Alternatives (only computed when fewer than `resultLimit` solutions fit) are ordered by
distance to the window → payable → units → key, and are always flagged `alternative = true` and
`withinRange = false` so no consumer can mistake them for valid solutions.

**Deduplication.** Two purchase plans that differ only by the order of their lines are the same plan, so
each combination is normalised to a canonical key such as `2:1|4:2` (ascending product id, zero
quantities omitted) and the key is used for identity and as the final tie-break. Because the DP emits one
candidate per VP level, duplicates cannot arise in the first place — the tests assert both properties.

### 6.7 Limits and complexity

* `n` = selected products, `C` = DP capacity in VP, `k = max_i(k_i + 1)` = largest unit count per product.

| Resource | Cost |
| --- | --- |
| Time | `O(n · C · k)` transition relaxations |
| Space | `O(n · C)` for the choice table (`int[n][C+1]`) plus `O(C)` for the two cost layers |
| Returned solutions | `≤ maximumResultLimit` (default 10, default output 3) |

Measured behaviour of the specification scenario (`n = 3`, `C ≈ 700`): a fraction of a millisecond, a few
thousand relaxed states. Every response carries the diagnostics that make the cost auditable:
`selectedProductCount`, `dpCapacity`, `exploredStates`, `reachableVpLevels`, `elapsedMillis`.

Recommended operating envelope (enforced, not just documented): **≤ 25 selected products**, accepted VP
window **≤ 20000 VP**, quantities `≤ maxQuantity` or `≤ 10` when unbounded. Requests outside the envelope
fail fast with `422 ENGINE_LIMIT_EXCEEDED` instead of degrading.

### 6.8 Engine API

```java
OptimizationResult result = optimizationEngine.optimize(new OptimizationRequest(
        List.of(new ProductOption(1L, "Protein Powder", "P001", "Supplements",
                new BigDecimal("2000.00"), 50, null, 6, SelectionType.ALLOWED, null)),
        PricingContext.global(new BigDecimal("20.00"), new BigDecimal("18.00")),
        500, ToleranceType.PERCENTAGE, new BigDecimal("10"), 3, limits));
```

The engine never touches the database, HTTP or the Spring context, which is why it is covered by fast
plain-JUnit tests (`src/test/java/.../optimization/**`) including a **brute-force oracle** cross-check on
randomized instances.

---

## 7. REST API

Swagger UI: `/swagger-ui.html` · OpenAPI JSON: `/v3/api-docs`

| Method & path | Purpose |
| --- | --- |
| `GET /api/products` | Paged search (`search`, `categoryId`, `active`, `sortBy`, `direction`, `page`, `size`) |
| `GET /api/products/{id}` | Single product |
| `POST /api/products` | Create (unique SKU, non-negative MRP/VP, sane quantity range) |
| `PUT /api/products/{id}` | Update |
| `PATCH /api/products/{id}/active` | Activate / deactivate (soft delete) |
| `DELETE /api/products/{id}` | Deactivate; `?permanent=true` hard-deletes only when no history references it |
| `GET /api/products/selection` | Active products for the selection screen |
| `GET/POST /api/categories`, `PUT/DELETE /api/categories/{id}` | Category CRUD (delete refused while in use) |
| `POST /api/optimizations` | Run an optimization and persist the session (`201` + `Location`) |
| `GET /api/optimizations` | Paged history (`search`, `page`, `size`) |
| `GET /api/optimizations/{id}` | Reopen a session exactly as it was calculated |
| `POST /api/pricing/preview` | Unit prices for a selection without optimizing |
| `GET /api/dashboard/summary` | Counts + latest sessions for the dashboard |
| `GET /api/health` | Liveness probe used by docker health checks and the UI |

### Run an optimization

```http
POST /api/optimizations
Content-Type: application/json

{
  "name": "Monthly VP target",
  "productIds": [1, 2, 4],
  "requiredProductIds": [],
  "discountPercent": 20,
  "gstPercent": 18,
  "targetVp": 500,
  "toleranceType": "PERCENTAGE",
  "toleranceValue": 10,
  "resultLimit": 3
}
```

```json
{
  "sessionId": 1,
  "name": "Monthly VP target",
  "createdAt": "2026-09-28T10:15:30Z",
  "status": "COMPLETED",
  "targetVp": 500,
  "minimumVp": 450,
  "maximumVp": 550,
  "toleranceType": "PERCENTAGE",
  "toleranceValue": 10.00,
  "discountPercent": 20.00,
  "gstPercent": 18.00,
  "requestedResultLimit": 3,
  "solutionCount": 3,
  "alternativeCount": 0,
  "message": "3 solution(s) found inside the accepted range 450 - 550 VP.",
  "selectedProducts": [
    {
      "productId": 2, "productName": "Multivitamin", "selectionType": "ALLOWED",
      "mrp": 1000.00, "volumePoint": 25,
      "catalogueMinQuantity": null, "catalogueMaxQuantity": 10,
      "effectiveMinQuantity": 0, "effectiveMaxQuantity": 10,
      "discountPercent": 20.00, "gstPercent": 18.00,
      "discountAmount": 200.00, "discountedPrice": 800.00,
      "gstAmount": 144.00, "finalUnitPrice": 944.00
    }
  ],
  "solutions": [
    {
      "rank": 1,
      "canonicalKey": "2:1|4:2",
      "explanation": "This combination exactly reaches your 500 VP target with a final payable cost of ₹12,272.00 using 3 units across 2 products.",
      "withinRange": true, "exactTarget": true, "alternative": false,
      "totalQuantity": 3, "totalVp": 500, "vpDifference": 0,
      "totalMrp": 13000.00, "totalDiscount": 2600.00, "totalGst": 1872.00,
      "finalPayableAmount": 12272.00, "costPerVp": 24.5440, "numberOfUniqueProducts": 2,
      "products": [
        {
          "productId": 2, "productName": "Multivitamin", "quantity": 1, "mrp": 3000.00,
          "volumePoint": 100, "totalVp": 100, "discountPercent": 20.00, "gstPercent": 18.00,
          "discountUnitAmount": 600.00, "discountedUnitPrice": 2400.00,
          "gstUnitAmount": 432.00, "finalUnitPrice": 2832.00,
          "totalProductCost": 2832.00, "required": false
        }
      ]
    }
  ],
  "closestAlternatives": [],
  "diagnostics": {
    "selectedProductCount": 3, "dpCapacity": 700,
    "exploredStates": 4321, "reachableVpLevels": 42, "elapsedMillis": 1
  }
}
```

When nothing fits the accepted window, `status` is `NO_VALID_SOLUTION`, `solutions` is empty and
`closestAlternatives` contains the nearest combinations **flagged as outside the requested range**.

---

## 8. Persistence, history and snapshots

Flyway owns the schema (`V1__init_schema.sql`, `V2__seed_demo_data.sql`); Hibernate runs with
`ddl-auto: validate`, so a drift between entities and SQL fails the start-up instead of silently
"fixing" the database.

| Table | Contents |
| --- | --- |
| `categories` | name (unique), description |
| `products` | name, SKU (unique), description, MRP, VP, min/max quantity, `active` flag, category FK, audit timestamps |
| `optimization_sessions` | inputs (target, tolerance, discount, GST, result limit, normalized VP window), status, counts, engine diagnostics, message |
| `optimization_session_products` | **snapshot** of every selected product: name, SKU, category name, required flag, MRP, VP and the effective min/max quantity the engine used |
| `optimization_results` | one row per returned solution: rank, in-range / exact / alternative flags, canonical key, totals, cost per VP, explanation |
| `optimization_result_lines` | one row per product line: quantity, MRP, discounted price, GST, final unit price, line total, line VP, `required` flag |

Why snapshots matter: MRPs change over time, but a session that was run last month must still show the
numbers that justified the purchase decision. Reopening a session therefore reads the stored snapshot
instead of recomputing against today's catalogue.

Products referenced by history are **soft-deleted** (`active = false`); a hard delete is refused with
`PRODUCT_IN_USE` while history still points at the product.

---

## 9. Frontend

React 19 + TypeScript + Vite + Material UI, React Router, Axios and React Hook Form.

* **Dashboard** — catalogue counts, latest sessions, mode indicator.
* **Products / Categories** — searchable, sortable, paged management with soft delete, quantity-range
  validation and SKU uniqueness feedback.
* **New optimization** — a two-step flow: (1) explicitly tick the products that may participate, mark any
  as *required*; (2) discount, GST, target VP, tolerance and the requested number of solutions. A live hint
  shows the normalized window, but the numbers used are always the backend's.
* **Results** — ranked solution cards (VP headline, payable, per-line breakdown), a side-by-side
  comparison table, the closest-alternatives block for out-of-range runs, CSV export and printing.
* **History / Settings** — reopen any session exactly as it was; configure defaults and the data source.

The UI **never calculates money**. It formats `BigDecimal` values coming from the API; the only
calculation in the browser is the pre-run hint of the accepted window.

**Demo mode (offline).** `src/engine/` is a faithful TypeScript port of the Java engine — same pricing
formulas, same VP normalization, same DP, same ranking and rounding (covered by the same specification
scenario in `npm test`). `src/services/demo/` implements the same API interface against `localStorage`,
so the application can be explored without Java or PostgreSQL; the header always shows whether you are
talking to the live backend or the in-browser engine.

---

## 10. Testing

### Backend (`backend/`)

```bash
mvn test        # unit + web layer tests (surefire; *IT excluded)
mvn verify      # adds the integration tests (failsafe; Testcontainers-free, H2 in PostgreSQL mode)
```

No Maven wrapper is committed to the repository; use an installed Maven 3.9+ (or run
`mvn -N wrapper:wrapper` once to generate `./mvnw` yourself).

| Suite | Focus |
| --- | --- |
| `optimization/calculator/*Test` | money policy, HALF_UP per step, the 2000/20%/18% example, zero GST/discount, cost per VP without division by zero |
| `optimization/model/*Test` | tolerance normalization (500 ± 10% → 450…550), canonical keys, quantity ranges |
| `optimization/engine/IntegerOptimizationEngineTest` | the specification scenario, constraints (REQUIRED, min/max quantity), zero-VP products, engine limits, plus a **brute-force oracle** over randomized instances |
| `optimization/ranking/*Test` | lexicographic ranking order, alternatives, explanation generation |
| `service/*Test`, `validation/*Test` | selection validation, error codes, service behaviour with mocked repositories |
| `OptimizationAcceptanceIT` | end-to-end: REST → service → engine → JPA → JSON, including the snapshot surviving an MRP change |
| `ProductCatalogueIT` | product/category lifecycle, validation errors, soft vs permanent delete, dashboard, health |

### Frontend (`frontend/`)

```bash
npm test           # vitest: engine parity tests (pricing, VP range, ranking, DP vs brute force)
npm run typecheck  # tsc -b (also part of npm run build)
npm run lint       # oxlint
```

---

## 11. Error codes

Every failure returns `{ "code", "message", "timestamp", "path" }` with an HTTP status; stack traces are
never serialized (`server.error.include-stacktrace: never`).

| Status | Codes |
| --- | --- |
| 400 | `VALIDATION_FAILED`, `MALFORMED_REQUEST`, `NO_PRODUCTS_SELECTED`, `INVALID_TARGET_VP`, `INVALID_DISCOUNT`, `INVALID_GST`, `INVALID_TOLERANCE`, `INVALID_RESULT_LIMIT`, `INVALID_SELECTION`, `INVALID_QUANTITY_RANGE`, `NO_SELECTABLE_PRODUCTS` |
| 404 | `PRODUCT_NOT_FOUND`, `CATEGORY_NOT_FOUND`, `OPTIMIZATION_SESSION_NOT_FOUND` |
| 409 | `PRODUCT_INACTIVE`, `PRODUCT_IN_USE`, `PRODUCT_SKU_ALREADY_EXISTS`, `CATEGORY_NAME_ALREADY_EXISTS`, `CATEGORY_IN_USE`, `DATA_INTEGRITY_VIOLATION` |
| 422 | `ENGINE_LIMIT_EXCEEDED` (too many products, target beyond the supported VP capacity) |
| 500 | `INTERNAL_ERROR` |

---

## 12. Deliberately out of scope (extensibility)

Not implemented, on purpose, to keep the first version honest and focused — the domain model already has
room for each of them:

* **Profitability per product** — `ProductOption` and `PricingContext` are the natural home for cost/margin fields.
* **Maximum budget constraint** — the DP capacity would gain a second axis, or budget becomes an additional pruning bound.
* **Per-product discount/GST** — `PricingContext.productOverrides` and `PricingCalculator` already support it; the request DTO just does not expose it yet.
* **Purchase-order batching, supplier splits, scheduling, authentication/authorization** — separate concerns that do not belong in the optimizer.

---

## 13. Project layout

```
vp-optimizer/
├── docker-compose.yml          PostgreSQL + backend + frontend
├── .env.example
├── render.yaml                 Render Blueprint: API container + PostgreSQL
├── DEPLOYMENT.md               Vercel / Netlify / Render / Railway / Fly / Cloud Run guide
├── backend/                    Java 21 · Spring Boot 3.5 · Maven
│   ├── Dockerfile · entrypoint.sh · fly.toml
│   └── src/{main,test}/...
└── frontend/                   React 19 · TypeScript · Vite · MUI
    ├── Dockerfile · nginx.conf · vercel.json · .nvmrc
    └── src/{engine,services,pages,components,context,hooks,types,utils}
```

---

## 14. Deployment

Three pieces, three kinds of host: the **frontend** is a static bundle for Vercel, Netlify, Cloudflare
Pages or a Render Static Site; the **backend** is a container for Render, Railway, Fly.io, Cloud Run or
a VPS; the **database** is managed PostgreSQL (Neon, Supabase, Render Postgres, …). Vercel and Netlify
cannot run the Spring Boot process, but the UI alone is a complete application because it falls back to
the in-browser engine.

| Path | Result | Time |
| --- | --- | --- |
| Frontend only (demo mode) | Public UI running the TypeScript engine against `localStorage` | ~3 min |
| Neon + Render (`render.yaml`) + Vercel/Netlify | Full stack, free tiers, real REST API and persisted history | ~15 min |
| Railway / Fly.io / Cloud Run | Backend alternatives with the same image | ~10 min |

Quick version — the frontend needs one value to go live (`VITE_API_BASE_URL=https://<api-host>/api`,
then redeploy so Vite rebuilds), and the backend needs one value to reach its database
(`DATABASE_URL`, translated by `backend/entrypoint.sh`). Everything else has working defaults.

**[→ Full deployment guide with step-by-step instructions, environment variables, verification and
troubleshooting](DEPLOYMENT.md)**

Built with Spring Boot 3.5, Java 21, PostgreSQL 16, Flyway, springdoc-openapi, React 19, TypeScript,
Vite, Material UI, Vitest, JUnit 5, Mockito and AssertJ.

# Deploying the VP Optimizer

This project is three things, and they deploy to **different kinds of platforms**:

| Part | What it is | Where it can run |
| --- | --- | --- |
| **Frontend** (`frontend/`) | Static React + Vite build (`dist/`) | Vercel, Netlify, Cloudflare Pages, Render Static Site, GitHub Pages, any web server |
| **Backend** (`backend/`) | Long-running Java 21 / Spring Boot 3 process in a Docker image | Render, Railway, Fly.io, Google Cloud Run, Azure App Service, AWS App Runner, a VPS |
| **Database** | PostgreSQL (schema created and seeded by Flyway) | Neon, Supabase, Render Postgres, Railway Postgres, Aiven, Cloud SQL, or a container |

> **Vercel and Netlify cannot host the Java backend.** Neither offers a JVM runtime or long-running
> processes, and their serverless functions have request timeouts that a Spring context + JPA +
> Flyway startup would exceed. They are excellent for the frontend — which is exactly how this
> repository is set up.

Two things make partial deployments painless by design:

* the UI **falls back to an in-browser demo engine** when no API answers, so a frontend-only deploy
  is a complete, explorable application (the header shows which mode is active);
* the backend image **translates a single `DATABASE_URL`** into JDBC settings at start-up
  (`backend/entrypoint.sh`), which is the format Render, Railway, Fly.io, Heroku and Neon hand out.

---

## 1. Fastest path: frontend only (about three minutes)

Demo mode runs the same engine in TypeScript — identical pricing formulas, tolerance normalization,
DP search, ranking and rounding — with data in `localStorage`. Perfect for a portfolio/demo link.

### Vercel

1. Push this repository to GitHub (see [step 2](#2-full-stack-neon--render--vercelnetlify)).
2. On [vercel.com](https://vercel.com) → **Add New… → Project → Import Git Repository**.
3. Set **Root Directory** to `vp-optimizer/frontend`. Vercel then reads
   `frontend/vercel.json` (framework `vite`, build `npm run build`, output `dist`, SPA rewrite).
4. (Optional) **Environment Variables** → `VITE_DATA_MODE` = `demo` to pin demo mode in public.
5. **Deploy**. Your site is live at `https://<project>.vercel.app`.

Client-side routes such as `/results/1001` work because of the `rewrites` entry in `vercel.json`.

### Netlify

1. [app.netlify.com](https://app.netlify.com) → **Add new site → Import an existing project** → GitHub.
2. Leave the base directory **empty**: the repository root already contains `netlify.toml`, which
   sets `base = "vp-optimizer/frontend"`, `command = "npm ci && npm run build"`, `publish = "dist"`
   and the SPA redirect.
3. **Deploy site**.

### Other static hosts

* **Cloudflare Pages**: build command `npm ci && npm run build`, output `dist`, root `vp-optimizer/frontend`.
* **Render Static Site**: build `npm ci && npm run build`, publish `vp-optimizer/frontend/dist`, and add
  a rewrite `/*` → `/index.html` (Render Dashboard → Redirects/Rewrites) so deep links work.
* **GitHub Pages**: set `base: '/<repo-name>/'` in `frontend/vite.config.ts` and publish `dist`
  (a GitHub Actions workflow is the usual way). Demo mode only.

---

## 2. Full stack: Neon + Render + Vercel/Netlify

Everything below fits in free tiers. Total time: ~15 minutes.

### Step 1 — Push the code to GitHub

All three platforms deploy from Git:

```bash
git add vp-optimizer
git commit -m "Add VP optimization system"
git push origin <your-branch>
```

### Step 2 — Create the database (Neon, permanent free tier)

1. [neon.com](https://neon.tech) → sign up → **Create project** (PostgreSQL 16, pick the region
   closest to your users, e.g. `ap-southeast-1` for Asia).
2. Copy the connection string from the dashboard:

   ```
   postgresql://neondb_owner:npg_xxx@ep-cool-name-123456.ap-southeast-1.aws.neon.tech/neondb?sslmode=require
   ```

That is the only database value you need — the backend entrypoint turns it into
`jdbc:postgresql://…/neondb?sslmode=require` plus the credentials. **Do not run any SQL**: Flyway
creates the schema and seeds six categories and seven demo products on first start.

Alternatives with a permanent free tier: **Supabase** (same connection-string format, add
`?sslmode=require`), **Aiven**. Render Postgres and Railway Postgres work too, but their free
instances expire / are trial credits.

### Step 3 — Deploy the backend (Render Blueprint)

1. Push, then [dashboard.render.com](https://dashboard.render.com) → **New + → Blueprint** → select
   the repository. Render reads `vp-optimizer/render.yaml` and shows a Postgres instance plus the
   `vp-optimizer-api` web service.
2. Before confirming, decide which database you want:
   * **Blueprint-managed Postgres** (default): leave the file as is. Free, 1 GB, but Render *deletes*
     free databases 30 days after creation (14-day grace period), so it is fine for a demo.
   * **Neon** (permanent): delete the whole `databases:` block from `render.yaml` and, in the
     Blueprint's environment prompt, set `DATABASE_URL` to the Neon string. Mark it as a secret.
3. **Apply**. Render builds `vp-optimizer/backend/Dockerfile` (`dockerContext` is
   `vp-optimizer/backend`), starts the container and waits for `GET /api/health` to answer `200`.
4. When the deploy finishes, your API is at `https://vp-optimizer-api.onrender.com`
   (Swagger UI: `/swagger-ui.html`).

**Manual alternative** (no Blueprint): **New + → Web Service** → connect the repo → Language
**Docker** → Dockerfile Path `vp-optimizer/backend/Dockerfile` → Docker Build Context Directory
`vp-optimizer/backend` → Health Check Path `/api/health` → Instance Type **Free** → add the
environment variables from [section 4](#4-environment-variables).

### Step 4 — Deploy the frontend and connect it

Deploy the UI as in [section 1](#1-fastest-path-frontend-only-about-three-minutes), then point it at
the API. Pick **one** of the two connection styles:

**A. Build-time API URL (recommended, works on every host)**

1. Frontend project → **Environment Variables** → add
   `VITE_API_BASE_URL = https://vp-optimizer-api.onrender.com/api`
2. Allow the frontend origin on the backend: Render → `vp-optimizer-api` → Environment →
   `CORS_ALLOWED_ORIGIN_PATTERNS = https://*.vercel.app,https://*.netlify.app`
   (or list exact origins with `CORS_ALLOWED_ORIGINS`). Save — Render redeploys automatically.
3. **Redeploy the frontend.** Vite inlines `VITE_*` variables at build time, so an existing
   deployment keeps the old value until it is rebuilt.

**B. Same-origin proxy (no CORS at all)**

Keep the frontend's relative `/api` calls and let the static host forward them.

*Netlify*: uncomment the `[[redirects]]` block in `netlify.toml`, replace the host, commit.

*Vercel*: add this rewrite **before** the catch-all one in `frontend/vercel.json`:

```json
{ "source": "/api/:path*", "destination": "https://vp-optimizer-api.onrender.com/api/:path*" }
```

*nginx* (own server): the image variant in `frontend/nginx.conf` already does this.

---

## 3. Alternative backends

### Railway

1. **New Project → Deploy from GitHub repo** → select the repository.
2. Service → **Settings → Root Directory** = `vp-optimizer/backend` (Railway detects the Dockerfile).
3. **New → Database → PostgreSQL**, then on the app service add the variable
   `DATABASE_URL = ${{Postgres.DATABASE_URL}}`.
4. Add `CORS_ALLOWED_ORIGIN_PATTERNS` (and optionally `JAVA_OPTS=-XX:MaxRAMPercentage=70`).
   Railway injects `PORT`; the app honours it. Public domain: **Settings → Networking → Generate Domain**.
   Railway has no free tier any more (trial credit, then ~$5/month).

### Fly.io

```bash
cd vp-optimizer/backend
fly launch --no-deploy --copy-config          # edit `app` in fly.toml to a unique name
fly postgres create --name vp-optimizer-db --region sin
fly postgres attach vp-optimizer-db           # sets DATABASE_URL for the app
fly secrets set CORS_ALLOWED_ORIGIN_PATTERNS="https://*.vercel.app,https://*.netlify.app"
fly deploy
```

`backend/fly.toml` already sets the internal port, the `/api/health` check and a 512 MB machine
(Spring Boot + Hikari + Flyway are not comfortable in 256 MB). Pay-as-you-go, no free tier.

### Google Cloud Run

```bash
gcloud run deploy vp-optimizer-api \
  --source vp-optimizer/backend \
  --region asia-south1 \
  --allow-unauthenticated \
  --memory 1Gi \
  --port 8080 \
  --set-env-vars DATABASE_URL="postgresql://user:pass@host/db?sslmode=require",CORS_ALLOWED_ORIGIN_PATTERNS="https://*.vercel.app"
```

Cloud Run injects `PORT` (8080 by default) and the app binds to it. Cloud SQL is possible but needs
the `cloud-sql-jdbc-socket-factory` dependency added to `pom.xml` — pointing Cloud Run at Neon is
simpler. Scale-to-zero means the first request after a while pays the JVM warm-up.

### Any Docker host / VPS

```bash
cd vp-optimizer
cp .env.example .env      # DB credentials + ports
docker compose up -d --build
```

Put your own reverse proxy (Caddy, Traefik, nginx) in front of port 3000 for TLS.

---

## 4. Environment variables

### Backend

| Variable | Required | Meaning |
| --- | --- | --- |
| `DATABASE_URL` | one of these three | Single connection string (`postgresql://user:pass@host:5432/db?sslmode=require`), translated to JDBC by `entrypoint.sh`. Ignored when `SPRING_DATASOURCE_URL` is set. |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | …or this | Discrete settings; used by `docker-compose.yml` and any host that gives you separate values. |
| `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | …or this | Standard Spring Boot overrides; use when you need extra JDBC parameters. |
| `CORS_ALLOWED_ORIGINS` | no | Comma separated exact browser origins allowed to call the API (default: the local Vite/preview ports). |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | no | Comma separated patterns, handy for wildcard platform domains (`https://*.vercel.app`). |
| `PORT` | injected by most PaaS | HTTP port; `server.port` reads `PORT`, then `SERVER_PORT`, then `8080`. |
| `JAVA_OPTS` | no | JVM flags. The image defaults to `-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport`. |
| `DB_POOL_SIZE` | no | Hikari maximum pool size (default 10). Neon's free tier likes a smaller value. |
| `SPRING_PROFILES_ACTIVE` | no | `docker` is the image default; `application-docker.yml` only changes defaults and logging. |

Anything else can be set the Spring way, for example to raise the engine safety limits:

```
SPRING_APPLICATION_JSON={"vp-optimizer":{"optimization":{"max-vp-capacity":50000,"max-selected-products":40}}}
```

### Frontend (build-time, Vite)

| Variable | Default | Meaning |
| --- | --- | --- |
| `VITE_API_BASE_URL` | `/api` | Absolute API URL for a live backend, e.g. `https://vp-optimizer-api.onrender.com/api`. Leave unset to use the same-origin `/api` (proxy or demo mode). |
| `VITE_DATA_MODE` | `auto` | `auto`, `api` or `demo`. `demo` never calls a backend — useful for a public showcase deploy. |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | **Dev/preview only**: where `npm run dev` proxies `/api`. Not used by a production build. |

> Vite bakes `VITE_*` values into the bundle: changing one requires a **new build/deploy**.

---

## 5. Verify a deployment

```bash
# 1. Backend liveness -> {"status":"UP","application":"vp-optimizer","version":"1.0.0"}
curl -s https://vp-optimizer-api.onrender.com/api/health

# 2. Seeded catalogue (Flyway V2) -> 7 products, 6 categories
curl -s "https://vp-optimizer-api.onrender.com/api/products?size=20" | head -c 400

# 3. The specification scenario -> 201 with 3 ranked solutions
curl -s -X POST https://vp-optimizer-api.onrender.com/api/optimizations \
  -H 'Content-Type: application/json' \
  -d '{"productIds":[1,2,4],"discountPercent":20,"gstPercent":18,"targetVp":500,
       "toleranceType":"PERCENTAGE","toleranceValue":10,"resultLimit":3,"name":"Deployed smoke test"}'
```

In the UI: open the deployed site, check the header chip says **Live backend API**
(Settings → *Re-check* if it was opened before the backend existed), run an optimization, then open
**History** and confirm the session reopens with its snapshot.

## 6. Troubleshooting

| Symptom | Cause / fix |
| --- | --- |
| Header says *Offline demo engine* although the API is up | `VITE_API_BASE_URL` is not set, is missing the `/api` suffix, or the frontend was not rebuilt after setting it. Cross-origin? The backend must allow the origin (see CORS below). |
| `No backend reachable` with a 200 response on `/api/health` | That 200 came from a static host serving `index.html`. The UI detects this (`isHealthPayload`) and stays in demo mode — configure the API URL or the `/api` rewrite. |
| Browser console: *blocked by CORS policy* | Add the exact origin to `CORS_ALLOWED_ORIGINS`, or a pattern to `CORS_ALLOWED_ORIGIN_PATTERNS`, then let the backend redeploy/restart. Trailing slashes or `http` vs `https` mismatches count. |
| Render build fails with `pom.xml not found` | Docker **Build Context** must be `vp-optimizer/backend` (the Dockerfile copies `pom.xml` and `src` from the context). |
| Startup fails with `Connection to … refused` / `UnknownHostException` | Wrong `DATABASE_URL`/`DB_*` values, or the database is not reachable from outside: use the provider's *internal* host for same-platform traffic, and keep `?sslmode=require` for managed Postgres. Free Render instances also start before the DB is ready — redeploy once. |
| `Schema-validation: missing table` / `missing column` | Hibernate runs with `ddl-auto: validate` and expects exactly the Flyway schema. Never edit the schema by hand: add a migration (`V3__…sql`). |
| 502/timeout on the first request | Cold start (Render free: ~1 minute after 15 min idle; Fly with `min_machines_running = 0`). Upgrade the plan or keep it warm with a scheduled ping. |
| `422 ENGINE_LIMIT_EXCEEDED` | More than 25 selected products, or a target VP above the DP capacity of 20000. Raise the limits with `SPRING_APPLICATION_JSON` (see section 4). |
| Optimizations are slow or the instance restarts | Free instances have 512 MB. Lower `DB_POOL_SIZE`, and keep `JAVA_OPTS` at `-XX:MaxRAMPercentage=70`. |
| Old sessions show old prices | That is intentional: results are stored as snapshots (see README §8). |

## 7. Free tier notes

| Platform | Free? | Catch |
| --- | --- | --- |
| Vercel / Netlify (frontend) | Yes | Hobby/personal plans; plenty for a static SPA |
| Render web service (backend) | Yes | 512 MB, 0.1 CPU, spins down after 15 min idle (~1 min cold start), 750 instance hours/workspace/month |
| Render Postgres | Yes | Expires 30 days after creation (14-day grace, then deleted) — use Neon if that matters |
| Neon / Supabase (database) | Yes | Permanent, small storage, compute suspends when idle (first query is slower) |
| Railway | No | Trial credit, then usage-based (~$5/month) |
| Fly.io | No | Usage-based; a 512 MB machine is a few dollars a month |

For a **public demo that never sleeps with no database dependence**, deploy the frontend only in
`VITE_DATA_MODE=demo` and skip the backend entirely.

## 8. Updating and redeploying

Vercel, Netlify and Render (with `autoDeployTrigger: commit`) redeploy on every push to the branch
they track. A backend redeploy re-runs Flyway, so new migrations apply automatically; the schema is
versioned, never destructive.

To roll back: Render → **Deploys → Rollback**; Vercel/Netlify → **Deployments → promote** an earlier
build. Database changes are not rolled back automatically — write migrations accordingly.

## 9. Before sharing a public link

* Replace wildcard CORS patterns with your exact frontend origins.
* The API is unauthenticated by design (the specification has no auth); keep it to demo data, or put
  it behind Cloudflare Access / a reverse proxy with basic auth / an API gateway.
* Keep credentials only in platform environment variables — never in the repository
  (`vp-optimizer/.gitignore` already excludes `.env`).
* Free managed databases are not backed up: `pg_dump` anything you care about.

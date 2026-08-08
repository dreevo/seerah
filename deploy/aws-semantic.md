# Seerah on AWS Free Tier — full features, semantic search ON

A 100%-AWS deploy that keeps **semantic search**, using two free-tier services:

```
Browser ─HTTPS─▶ EC2 t3.micro (1 GB)                RDS db.t3.micro
                 ├─ Caddy: SPA + /api proxy + TLS    └─ PostgreSQL, 20 GB
                 └─ Spring Boot + ONNX search  ──5432──▶ (private, EC2 SG only)
```

It fits 1 GB because (a) the **database is on RDS**, not the app box, and (b) semantic search
uses the **22 MB int8 model** (`SEERAH_SEARCH_MODEL=model-int8.onnx`) instead of the 86 MB one —
same results, ~130 MB less memory.

**Free for 12 months** (both EC2 *and* RDS free tiers are 12‑month). After that a `t3.micro` +
`db.t3.micro` run ~$20/mo, so set a budget alert and move on before then if you want to stay free.
Everything below stays inside the free tier if you keep the defaults (single instance, single‑AZ,
≤20 GB, no Multi‑AZ).

---

## 1. Create the database (RDS)
RDS → **Create database**:
- **Standard create**, engine **PostgreSQL**.
- **Templates → Free tier** (this pins `db.t3.micro`, 20 GB, Single‑AZ automatically).
- **Master username** `seerah`, set a strong **master password**.
- **Additional configuration → Initial database name:** `seerah`.
- **Connectivity:** default VPC; **Public access = No** (only the EC2 box will reach it).
- Create it, then open the instance and copy its **Endpoint** (e.g.
  `seerah.xxxx.eu-west-1.rds.amazonaws.com`). You'll wire its security group in step 5.

## 2. Create the server (EC2)
EC2 → **Launch instance**:
- **AMI** Ubuntu Server 24.04 LTS.
- **Type** `t3.micro` (free‑tier eligible, 1 GB).
- **Key pair** for SSH.
- **Storage** 20–30 GB gp3 (30 GB EBS is free).
- **Security group (inbound):** SSH `22` from *your IP*, HTTP `80` and HTTPS `443` from `0.0.0.0/0`.
- Launch, then **allocate an Elastic IP** and associate it (stable public IP; free while attached).

## 3. Public URL — no domain required
You don't need to buy a domain. Pick one:

- **Free HTTPS via nip.io (recommended):** `nip.io` resolves `<ip>.nip.io` to your IP, so
  Caddy can still get a real certificate. In `.env` (step 6) set
  `SITE_DOMAIN=<EC2-PUBLIC-IP>.nip.io` (e.g. `52.14.203.11.nip.io`). Your URL is then
  `https://<EC2-PUBLIC-IP>.nip.io`. (Needs 80 **and** 443 open — step 2 did that.)
- **Plain HTTP:** set `SITE_DOMAIN=:80`. URL is `http://<EC2-PUBLIC-IP>` — works fully, just no
  padlock.
- **Own a domain?** Point an **A record** at the Elastic IP and use it as `SITE_DOMAIN`.

Either way, skip to step 4 — nothing else to configure.

## 4. Prepare the server
SSH in (`ssh -i key.pem ubuntu@<elastic-ip>`), then:

Swap — mainly so the **image builds** finish on 1 GB (runtime barely needs it):
```bash
sudo fallocate -l 3G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```
Docker:
```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER && newgrp docker
```

## 5. Let the app reach RDS (security groups)
In the EC2 console, note the instance's **security group ID** (e.g. `sg-app…`).
Open the **RDS instance → Connectivity & security → its VPC security group → Inbound rules →
Edit**: add **Type PostgreSQL (TCP 5432), Source = the EC2 security group** (`sg-app…`).
Do *not* open 5432 to the internet.

## 6. Configure
```bash
git clone <your-repo-url> seerah && cd seerah/deploy
cp .env.aws.example .env
nano .env
```
Set:
- `DB_URL=jdbc:postgresql://<rds-endpoint>:5432/seerah?sslmode=require`
- `DB_USER` / `DB_PASSWORD` = the RDS master creds from step 1
- `SITE_DOMAIN`, `ACME_EMAIL`

## 7. Launch
```bash
docker compose -f docker-compose.aws.yml --env-file .env up -d --build
```
The build takes ~10–15 min on 1 GB (Gradle + `ng build` lean on swap). Then it seeds RDS and
builds the search index:
```bash
docker compose -f docker-compose.aws.yml logs -f backend    # wait for "Started SeerahApplication"
```
Open **https://your-domain** — full app, **semantic search working**, over HTTPS.

---

## Keep it free — guardrails
- RDS: **Single‑AZ, `db.t3.micro`, ≤20 GB** (the Free‑tier template enforces this). No Multi‑AZ.
- EC2: **one** `t3.micro`. Elastic IP is free only while attached to a running instance —
  release it if you ever stop the instance long‑term.
- **Billing → Budgets → create a $1 budget with an email alert** as a safety net.
- Both free tiers end at **12 months** from account creation.

## Operations
**Update**
```bash
cd seerah && git pull && cd deploy
docker compose -f docker-compose.aws.yml --env-file .env up -d --build
```
**Back up the database** — RDS takes automated daily snapshots (free within the 20 GB backup
allowance); for a manual dump:
```bash
docker compose -f docker-compose.aws.yml exec backend sh -c \
  'apk add --no-cache postgresql-client >/dev/null; pg_dump "$DB_URL_PSQL"'   # or run pg_dump from your laptop against the RDS endpoint
```
(Simplest: `pg_dump -h <rds-endpoint> -U seerah seerah > backup.sql` from any machine whose IP
you temporarily allow on the RDS security group.)

**Logs / stop**
```bash
docker compose -f docker-compose.aws.yml ps
docker compose -f docker-compose.aws.yml logs -f web        # Caddy / TLS
docker compose -f docker-compose.aws.yml down               # stop (RDS data persists on RDS)
```

## Troubleshooting
- **Backend can't reach the DB** (`connection refused` / timeout): the RDS security group isn't
  allowing 5432 from the EC2 security group (step 5), or RDS `Public access` differs from the
  EC2's VPC. Both must be in the same VPC.
- **No certificate:** ports 80/443 not open (step 2) or DNS not pointing at the Elastic IP
  (step 3). `docker compose … logs -f web` shows the ACME error.
- **Build killed / OOM on 1 GB:** raise swap to 4–5 GB for the build, or build the two images on
  your laptop and push to a free registry (GHCR/Docker Hub), then `docker compose pull` on the box.
- **Want to drop memory further:** set `SEERAH_SEARCH_SEMANTIC=false` (keyword search, no model)
  — but with RDS + the quantized model you shouldn't need to.

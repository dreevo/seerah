# Deploying Seerah to Oracle Cloud (Always Free) — full features, $0

This runs the **whole platform** — Postgres + the Spring Boot API with **semantic
search kept on** + the Angular app — on a single Always‑Free Ampere (ARM) VM, with
Caddy providing automatic HTTPS. One origin serves the site and proxies the API, so
there is no CORS to configure.

```
Browser ──HTTPS──▶ Caddy (web) ──┬─▶ /api/*  → backend:8080 (Spring Boot + ONNX search)
                                 └─▶ /*      → Angular SPA (static files)
                                        backend ─▶ db:5432 (Postgres, private volume)
```

Everything is ARM64‑ready: `eclipse-temurin:21`, ONNX Runtime 1.20, DJL tokenizers and
`postgres:16`/`caddy`/`node` all publish `linux/arm64` images, and you build on the VM
itself, so the native architecture is automatic.

---

## 1. Create the VM

1. Sign up at <https://cloud.oracle.com> (a card is needed for identity; Always‑Free
   resources are never charged).
2. **Compute → Instances → Create instance.**
   - **Image:** Canonical **Ubuntu 22.04**.
   - **Shape:** *Ampere* → **VM.Standard.A1.Flex**, e.g. **2 OCPU / 12 GB** (Always Free
     covers up to 4 OCPU / 24 GB total — 2/12 is plenty).
   - Add your **SSH public key**.
   - Leave it on the default VCN/subnet with a **public IPv4**.
3. Note the instance's **public IP**.

## 2. Open the ports (two places — Oracle blocks both)

**a) Cloud firewall (VCN Security List):** Networking → your VCN → the subnet's Security
List → **Add Ingress Rules**: source `0.0.0.0/0`, TCP, destination ports **80** and **443**.

**b) Host firewall (Oracle's Ubuntu images drop by default):**
```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80  -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save   # (sudo apt install -y iptables-persistent  if missing)
```

## 3. Point DNS at the VM

Create an **A record** for your domain (e.g. `seerah.example.com`) → the VM's public IP.
Wait until `dig +short seerah.example.com` returns that IP before starting — Caddy needs
DNS to resolve to obtain a certificate. (No domain? See *Testing without a domain* below.)

## 4. Install Docker

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER && newgrp docker      # run docker without sudo
```

## 5. Get the code and configure

```bash
git clone <your-repo-url> seerah && cd seerah/deploy
cp .env.example .env
nano .env        # set a strong DB_PASSWORD, your SITE_DOMAIN and ACME_EMAIL
```

## 6. Launch

```bash
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

First boot takes a few minutes: it builds the images, migrates the schema, seeds the
Qur'an + all chronicles, and builds the in‑memory search index. Watch it:

```bash
docker compose -f docker-compose.prod.yml logs -f backend
# wait for:  Started SeerahApplication ... and the seeding lines to finish
```

Then open **https://your-domain** — the timeline, event pages, ḥadīth trees, map, and
semantic search all work, over HTTPS.

---

## Operations

**Update to a new version**
```bash
cd seerah && git pull && cd deploy
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

**Back up / restore the database**
```bash
docker compose -f docker-compose.prod.yml exec db pg_dump -U "$DB_USER" "$DB_NAME" > backup.sql
# restore:  cat backup.sql | docker compose ... exec -T db psql -U "$DB_USER" "$DB_NAME"
```

**Logs / status / stop**
```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f web       # Caddy / TLS
docker compose -f docker-compose.prod.yml down              # stop (keeps volumes/data)
```

## Notes & troubleshooting

- **Re‑seeding:** the seeder is idempotent (it checks whether data already exists), so
  `SEERAH_SEED=true` is safe to leave on. To wipe and re‑seed, `down -v` (deletes the DB
  volume) then `up`.
- **Certificate didn't issue:** it's almost always ports (step 2, *both* places) or DNS
  not yet pointing at the VM (step 3). `docker compose ... logs -f web` shows the ACME error.
- **Memory:** the backend is capped at 2 GB (`mem_limit`), ~1.1 GB JVM heap + the ONNX
  model's off‑heap native memory. Comfortable on a 12 GB VM. If you ever run on a tiny
  box and need to drop the 86 MB model, that's the one thing to change — ask and I'll add
  a `SEERAH_SEARCH_SEMANTIC=false` flag with a keyword‑search fallback.
- **Testing without a domain:** set `SITE_DOMAIN=:80` in `.env` (Caddy serves plain HTTP
  on the IP, no TLS) to smoke‑test, then switch to a real domain for HTTPS.

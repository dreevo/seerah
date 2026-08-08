# Deploying Seerah to AWS Free Tier

Yes, it runs on AWS free tier — with two honest caveats vs. Oracle:

1. **Free for 12 months only** (then a t3.micro is ~$7.50/mo). Oracle's ARM tier is free
   *forever*. If you want permanent-free, prefer Oracle (`README.md`).
2. **1 GB RAM.** The only free-tier compute is **EC2 t3.micro (1 GB)**. This guide keeps it
   simple with **keyword search** on a single box. **Want semantic search on AWS free tier
   anyway?** It fits if you move the DB to RDS and use the 22 MB quantized model — see
   [`aws-semantic.md`](aws-semantic.md). Everything else (timeline, event pages, ḥadīth trees,
   map, chronicles) is identical either way.

Same one-container-origin design as Oracle (Caddy serves the SPA + proxies `/api`, auto
HTTPS); only the sizing env differs.

---

## 1. Launch the instance
EC2 → **Launch instance**:
- **AMI:** Ubuntu Server 24.04 LTS.
- **Type:** **t3.micro** (free-tier eligible; 1 GB). *(t4g.micro / ARM is also fine — you
  build on the box, so either arch works.)*
- **Key pair:** create/select one for SSH.
- **Storage:** 20–30 GB gp3 (30 GB EBS is free).
- **Security group — inbound rules:** SSH `22` (your IP only), HTTP `80` and HTTPS `443`
  (`0.0.0.0/0`).
- (Recommended) allocate an **Elastic IP** and associate it, so the public IP is stable
  (free while attached to a running instance).

## 2. DNS
Create an **A record** for your domain → the instance's public/Elastic IP. Confirm with
`dig +short your-domain` before starting (Caddy needs it to resolve for the certificate).

## 3. SSH in and add swap (important on 1 GB)
Swap lets the JVM run and — crucially — lets `ng build` / the Gradle build finish on 1 GB.
```bash
sudo fallocate -l 3G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h        # should show 3.0Gi swap
```

## 4. Install Docker
```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER && newgrp docker
```

## 5. Configure for a small box
```bash
git clone <your-repo-url> seerah && cd seerah/deploy
cp .env.example .env
nano .env
```
Set `DB_PASSWORD`, `SITE_DOMAIN`, `ACME_EMAIL`, **and uncomment the three sizing overrides**
so it fits 1 GB:
```
SEERAH_SEARCH_SEMANTIC=false
JAVA_OPTS=-XX:MaxRAMPercentage=70
BACKEND_MEM=750m
```

## 6. Build & launch
```bash
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```
On 1 GB the **image build is slow** (Gradle + `ng build` lean on swap) — allow 10–20 min the
first time. Then it seeds and starts:
```bash
docker compose -f docker-compose.prod.yml logs -f backend   # wait for "Started SeerahApplication"
```
Open **https://your-domain**. Search now uses keyword matching; everything else is full.

> Build keeps OOM-ing? Either raise swap to 4–5 GB for the build, or build the two images on
> your laptop and push them to a free registry (Docker Hub / GHCR), then `docker compose pull`
> on the instance instead of `--build`.

---

## Optional: offload Postgres to RDS (frees RAM, still free 12 mo)
Run the DB outside the tiny box so the instance only runs the app + Caddy:
1. RDS → create **PostgreSQL**, **db.t3.micro**, 20 GB (free tier), same VPC; allow the EC2
   security group to reach port 5432.
2. In `.env`, point at it and drop the local DB:
   ```
   # DB_URL is built from these in compose; instead set it directly for RDS:
   ```
   Edit `docker-compose.prod.yml`: remove the `db` service + its `depends_on`, and set
   `DB_URL: jdbc:postgresql://<rds-endpoint>:5432/<DB_NAME>?sslmode=require`.
3. `up -d --build` as before.

## Operations
Same as Oracle (`README.md`): update with `git pull && … up -d --build`; back up with
`pg_dump`; certificate issues are almost always ports (step 1 security group) or DNS (step 2).

## Recommendation
For a **portfolio/demo that's free for a year and you don't mind keyword search**, AWS t3.micro
is fine. For **free-forever with full semantic search**, use **Oracle Always Free** instead —
same repo, just leave the sizing overrides commented out.

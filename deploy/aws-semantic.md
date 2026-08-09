# Seerah on AWS Free Tier — Full Features, Semantic Search ON

A 100%-AWS deployment that keeps **semantic search**, using two free-tier services:

```text
Browser ─HTTP─▶ EC2 t3.micro (1 GB)
                  ├─ Caddy: SPA + /api proxy
                  └─ Spring Boot + ONNX search
                           │
                           │ 5432 (private)
                           ▼
                    RDS db.t3.micro
                    PostgreSQL, 20 GB
```

The database is on **RDS**, not the EC2 box, and semantic search uses the **22 MB int8 model**:

```text
SEERAH_SEARCH_MODEL=model-int8.onnx
```

This keeps the runtime small enough for the 1 GB `t3.micro`.

The application is accessed directly through the EC2 **Elastic IP**, so **no domain, Route 53, or DNS configuration is required**.

> **Note:** Without a domain, this setup uses HTTP rather than HTTPS. Caddy/Let's Encrypt cannot provide a normal certificate for a bare IP address.

---

# 1. Create the database (RDS)

AWS Console → **RDS → Create database**

Use:

* **Standard create**
* Engine: **PostgreSQL**
* Template: **Free tier**
* DB instance: `db.t3.micro`
* Storage: **20 GB**
* Single-AZ
* Master username: `seerah`
* Set a strong master password
* **Initial database name:** `seerah` if available
* Connectivity:

  * Default VPC
  * **Public access = No**

Create the database.

After creation, open the RDS instance and copy its **Endpoint**.

Example:

```text
seerah.xxxxx.eu-north-1.rds.amazonaws.com
```

### Important: create the application database if it doesn't exist

RDS creates the PostgreSQL server, but the application database itself may not exist depending on how the RDS instance was created.

Test connectivity from EC2 first.

From the EC2 server:

```bash
psql "host=<RDS_ENDPOINT> port=5432 dbname=postgres user=seerah sslmode=require"
```

Then create the application database:

```sql
CREATE DATABASE seerah;
```

Exit:

```sql
\q
```

Test:

```bash
psql "host=<RDS_ENDPOINT> port=5432 dbname=seerah user=seerah sslmode=require"
```

You should get:

```text
seerah=>
```

> `SEERAH_SEED=true` in Docker Compose initializes/seeds the application's schema/data. It does not necessarily create the PostgreSQL database itself.

---

# 2. Create the EC2 server

AWS Console → **EC2 → Launch instance**

Use:

* AMI: **Ubuntu Server 24.04 LTS**
* Instance type: **t3.micro**
* Key pair: create/select your SSH key
* Storage: **20–30 GB gp3**
* Security group:

  * SSH `22` from **your IP**
  * HTTP `80` from `0.0.0.0/0`
  * HTTPS `443` from `0.0.0.0/0`

Launch the instance.

---

# 3. Allocate an Elastic IP

Because there is no domain, the application will be accessed directly through the EC2 Elastic IP.

AWS Console:

**EC2 → Network & Security → Elastic IPs**

1. Click **Allocate Elastic IP address**
2. Keep the default Amazon IPv4 pool
3. Allocate
4. Select the new Elastic IP
5. **Actions → Associate Elastic IP address**
6. Select your EC2 instance

After association, EC2 should show something like:

```text
Public IPv4 address: 16.xxx.xxx.xxx
Elastic IP address: 16.xxx.xxx.xxx
```

The application will eventually be available at:

```text
http://16.xxx.xxx.xxx
```

> Elastic IP pricing/free-tier rules can change. Release the Elastic IP if you no longer need it.

---

# 4. Connect to EC2

SSH into the server:

```bash
ssh -i key.pem ubuntu@<ELASTIC_IP>
```

---

# 5. Configure swap

A `t3.micro` has only about 1 GB RAM. Swap is useful during the Docker image builds.

Create 3 GB swap:

```bash
sudo fallocate -l 3G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

Verify:

```bash
free -h
```

You should see approximately:

```text
Mem:   ~908Mi
Swap:  3.0Gi
```

---

# 6. Install Docker

```bash
curl -fsSL https://get.docker.com | sudo sh
```

Add the current user to the Docker group:

```bash
sudo usermod -aG docker $USER
newgrp docker
```

Verify:

```bash
docker --version
```

Test:

```bash
docker run hello-world
```

---

# 7. Clone the project

Clone into the Ubuntu user's home directory rather than `/opt`:

```bash
cd ~
git clone https://github.com/dreevo/seerah.git
cd seerah
```

The project will be located at:

```text
/home/ubuntu/seerah
```

Go to the deployment directory:

```bash
cd ~/seerah/deploy
```

---

# 8. Configure the RDS security group

The RDS database must remain private.

In AWS:

**RDS → Databases → seerah → Connectivity & security**

Find the RDS **VPC security group**.

Open:

**Inbound rules → Edit inbound rules → Add rule**

Configure:

```text
Type:        PostgreSQL
Protocol:    TCP
Port:        5432
Source:      EC2 instance security group
```

For example:

```text
PostgreSQL | TCP | 5432 | sg-xxxxxxxx
```

Do **not** use:

```text
0.0.0.0/0
```

The EC2 instance should be the only thing allowed to access PostgreSQL.

---

# 9. Test EC2 → RDS connectivity

Install the PostgreSQL client if necessary:

```bash
sudo apt update
sudo apt install -y postgresql-client
```

First test the port:

```bash
nc -vz <RDS_ENDPOINT> 5432
```

Expected:

```text
Connection to <RDS_ENDPOINT> 5432 port [tcp/postgresql] succeeded!
```

Then test PostgreSQL:

```bash
psql "host=<RDS_ENDPOINT> port=5432 dbname=seerah user=seerah sslmode=require"
```

If successful:

```text
seerah=>
```

Exit:

```sql
\q
```

---

# 10. Configure `.env`

Create the environment file:

```bash
cd ~/seerah/deploy
cp .env.aws.example .env
nano .env
```

Use:

```env
DB_URL=jdbc:postgresql://<RDS_ENDPOINT>:5432/seerah?sslmode=require
DB_USER=seerah
DB_PASSWORD=<RDS_MASTER_PASSWORD>

SITE_DOMAIN=<ELASTIC_IP>
ACME_EMAIL=
```

Example:

```env
DB_URL=jdbc:postgresql://seerah.xxxxx.eu-north-1.rds.amazonaws.com:5432/seerah?sslmode=require
DB_USER=seerah
DB_PASSWORD=your-real-password

SITE_DOMAIN=16.xxx.xxx.xxx
ACME_EMAIL=
```

> Never commit `.env` to Git. It contains the database password.

---

# 11. Configure Caddy for IP-only HTTP

The original Caddy configuration expects a domain and Let's Encrypt certificate.

Since this deployment has **no domain**, replace:

```text
~/seerah/deploy/Caddyfile
```

with:

```caddyfile
:80 {
    encode zstd gzip

    # API + health checks -> Spring Boot backend
    @backend path /api/* /actuator/*
    handle @backend {
        reverse_proxy backend:8080
    }

    # Angular SPA
    handle {
        root * /srv
        try_files {path} /index.html
        file_server
    }
}
```

This means:

```text
http://<ELASTIC_IP>
        │
        ▼
    Caddy :80
        │
        ├── Angular SPA
        │
        └── /api/* → Spring Boot :8080
```

There is no Let's Encrypt certificate and no HTTPS in this version.

---

# 12. Verify Docker Compose configuration

Before starting the application, verify what Docker Compose will actually use:

```bash
cd ~/seerah/deploy
docker compose -f docker-compose.aws.yml --env-file .env config
```

You can specifically check the important variables without displaying the database password:

```bash
docker compose -f docker-compose.aws.yml --env-file .env config | grep -E 'DB_URL|DB_USER|SEERAH_|SITE_DOMAIN|ACME_EMAIL'
```

You should see values corresponding to:

```text
DB_URL: jdbc:postgresql://<RDS_ENDPOINT>:5432/seerah?sslmode=require
DB_USER: seerah
SEERAH_SEED: "true"
SEERAH_SEARCH_SEMANTIC: "true"
SEERAH_SEARCH_MODEL: model-int8.onnx
SITE_DOMAIN: <ELASTIC_IP>
```

Validate the Compose configuration:

```bash
docker compose -f docker-compose.aws.yml --env-file .env config --quiet
```

No output means the configuration is valid.

---

# 13. Start the application

Run:

```bash
docker compose -f docker-compose.aws.yml --env-file .env up -d --build
```

The build can take approximately 10–15+ minutes on a 1 GB `t3.micro`.

Swap is primarily there to make the build possible.

The important runtime settings are:

```yaml
SEERAH_SEED: "true"
SEERAH_SEARCH_SEMANTIC: "true"
SEERAH_SEARCH_MODEL: "model-int8.onnx"
JAVA_TOOL_OPTIONS: "-Xmx448m"
```

Semantic search therefore remains enabled.

---

# 14. Check the backend

```bash
docker compose -f docker-compose.aws.yml ps
```

Then:

```bash
docker compose -f docker-compose.aws.yml logs -f backend
```

Wait for:

```text
Started SeerahApplication
```

The first boot should also seed the RDS database.

---

# 15. Open the application

Find your EC2 Elastic IP and open:

```text
http://<ELASTIC_IP>
```

Example:

```text
http://16.xxx.xxx.xxx
```

The application should load with:

* Angular frontend
* Spring Boot backend
* RDS PostgreSQL
* Semantic search
* ONNX int8 model

---

# 16. Architecture

The final deployment looks like:

```text
                         Internet
                            │
                            │ HTTP :80
                            ▼
                 ┌─────────────────────┐
                 │ EC2 t3.micro        │
                 │ 1 GB RAM + 3 GB     │
                 │ swap                │
                 │                     │
                 │ Caddy :80           │
                 │    │                │
                 │    ├─ Angular SPA   │
                 │    │                │
                 │    └─ /api/*        │
                 │          │          │
                 │      Spring Boot    │
                 │      ONNX Search    │
                 └──────────┼──────────┘
                            │
                         TCP 5432
                     private VPC only
                            │
                            ▼
                 ┌─────────────────────┐
                 │ RDS PostgreSQL      │
                 │ db.t3.micro         │
                 │ 20 GB               │
                 │ Single-AZ            │
                 │                     │
                 │ database: seerah    │
                 └─────────────────────┘
```

---

# Keep it free

### RDS

Keep:

* `db.t3.micro`
* Single-AZ
* ≤20 GB storage
* No Multi-AZ
* No unnecessary additional instances

### EC2

Keep:

* One `t3.micro`
* One instance
* 20–30 GB gp3

### Elastic IP

The Elastic IP is used so the application has a stable address.

Release it when you permanently stop using the deployment.

### Billing protection

Create an AWS budget:

**AWS Console → Billing → Budgets → Create budget**

Set a low-cost alert such as:

```text
$1
```

and enable email notifications.

> AWS pricing and Free Tier rules can change, so check the current AWS pricing page before relying on a specific “12 months / $0” assumption.

---

# Operations

## Update

```bash
cd ~/seerah
git pull

cd deploy
docker compose -f docker-compose.aws.yml --env-file .env up -d --build
```

## Check containers

```bash
cd ~/seerah/deploy
docker compose -f docker-compose.aws.yml ps
```

## Backend logs

```bash
docker compose -f docker-compose.aws.yml logs -f backend
```

## Caddy/frontend logs

```bash
docker compose -f docker-compose.aws.yml logs -f web
```

## Stop the application

```bash
docker compose -f docker-compose.aws.yml down
```

This stops/removes the application containers but **does not delete the RDS database**.

---

# Database backup

RDS provides automated backups according to the configured retention period.

For an additional manual backup, run `pg_dump` from a machine that can access the RDS instance:

```bash
pg_dump -h <RDS_ENDPOINT> -U seerah seerah > backup.sql
```

If running from your laptop, you would need to temporarily allow your IP in the RDS security group.

---

# Troubleshooting

## EC2 cannot reach RDS

Test:

```bash
nc -vz <RDS_ENDPOINT> 5432
```

If it times out:

* Check the RDS security group.
* Make sure TCP `5432` allows the EC2 security group.
* Make sure EC2 and RDS are in the same VPC.
* Confirm RDS is private but reachable from the EC2 subnet/security group.

Do **not** open PostgreSQL to the entire internet.

---

## `database "seerah" does not exist`

Connect to the default PostgreSQL database:

```bash
psql "host=<RDS_ENDPOINT> port=5432 dbname=postgres user=seerah sslmode=require"
```

Then:

```sql
CREATE DATABASE seerah;
```

The application can then connect to it and perform its schema/data initialization.

---

## Docker build gets killed / OOM

The `t3.micro` only has around 1 GB RAM.

Check:

```bash
free -h
```

Make sure swap exists:

```bash
swapon --show
```

If necessary, increase swap:

```bash
sudo swapoff /swapfile
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

Verify:

```bash
free -h
```

Alternatively, build the Docker images on another machine and push them to a container registry, then pull them on EC2.

---

## Application cannot connect to PostgreSQL

Check the resolved Compose configuration:

```bash
docker compose -f docker-compose.aws.yml --env-file .env config
```

Verify:

```text
DB_URL
DB_USER
DB_PASSWORD
```

The JDBC URL should look like:

```text
jdbc:postgresql://<RDS_ENDPOINT>:5432/seerah?sslmode=require
```

---

## Browser cannot open the application

Check:

```bash
docker compose -f docker-compose.aws.yml ps
```

Make sure the `web` container is running.

Then check:

```bash
docker compose -f docker-compose.aws.yml logs -f web
```

Also verify the EC2 security group allows:

```text
TCP 80 → 0.0.0.0/0
```

And access:

```text
http://<ELASTIC_IP>
```

not:

```text
https://<ELASTIC_IP>
```

---

## Want HTTPS later?

The IP-only deployment intentionally uses HTTP.

For HTTPS, purchase/use a domain and point an `A` record at the Elastic IP. Then restore the domain-based Caddy configuration with Let's Encrypt.

That version can provide:

```text
https://your-domain.com
```

with automatic certificate management.

---

# Final configuration

The important production settings are:

```yaml
SEERAH_SEED: "true"
SEERAH_SEARCH_SEMANTIC: "true"
SEERAH_SEARCH_MODEL: "model-int8.onnx"
JAVA_TOOL_OPTIONS: "-Xmx448m"
```

Database:

```text
RDS PostgreSQL
db.t3.micro
20 GB
Single-AZ
Private
```

Application:

```text
EC2 t3.micro
1 GB RAM
3 GB swap
Docker
Caddy
Spring Boot
ONNX semantic search
```

Access:

```text
http://<EC2_ELASTIC_IP>
```

No domain or Route 53 is required for this version.

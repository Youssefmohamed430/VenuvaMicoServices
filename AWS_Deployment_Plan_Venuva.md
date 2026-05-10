# AWS Console Deployment Plan — Venuva Microservices

> **Project:** Venuva Event Management Platform  
> **Architecture:** 5 Spring Boot Microservices + RabbitMQ  
> **Services:** Auth (8081) · Event (8088) · Registration (8085) · Notification (9090) · Payment (9099)

---

## Table of Contents

1. [Region & Availability Zones](#1-region--availability-zones)
2. [VPC & Networking](#2-vpc--networking)
3. [Security Groups](#3-security-groups)
4. [IAM Roles & Policies](#4-iam-roles--policies)
5. [RDS — MySQL Databases](#5-rds--mysql-databases)
6. [S3 Bucket](#6-s3-bucket)
7. [EC2 — Backend (Private Subnets)](#7-ec2--backend-private-subnets)
8. [Auto Scaling Group](#8-auto-scaling-group)
9. [NAT Gateway](#9-nat-gateway)
10. [Load Balancer (ALB)](#10-load-balancer-alb)
11. [API Gateway](#11-api-gateway)
12. [EC2 — Frontend (Public Subnet)](#12-ec2--frontend-public-subnet)
13. [CloudWatch — Monitoring & Alerts](#13-cloudwatch--monitoring--alerts)
14. [Deployment Order Checklist](#14-deployment-order-checklist)
15. [Architecture Diagram (Text)](#15-architecture-diagram-text)

---

## 1. Region & Availability Zones

### Recommended Region
- **Region:** `me-south-1` (Bahrain) — closest to Egypt/Middle East, lowest latency for your users.
- **Alternative:** `eu-west-1` (Ireland) if Bahrain doesn't have all required services.

### Availability Zones (AZs)
Use **2 AZs** for high availability at reasonable cost:

| AZ | Label |
|---|---|
| `me-south-1a` | Primary |
| `me-south-1b` | Secondary (failover) |

> Go to: **AWS Console → EC2 → top-right region selector → Middle East (Bahrain)**

---

## 2. VPC & Networking

### Step 2.1 — Create VPC

1. Go to **VPC → Your VPCs → Create VPC**
2. Fill in:
   - **Name tag:** `venuva-vpc`
   - **IPv4 CIDR block:** `10.0.0.0/16`
   - **Tenancy:** Default
3. Click **Create VPC**
4. After creation, select it → **Actions → Edit VPC Settings** → enable **DNS hostnames** and **DNS resolution**

---

### Step 2.2 — Create Subnets

Create **6 subnets** total (3 per AZ):

#### Public Subnets (for frontend EC2 + NAT Gateway + Load Balancer)

| Name | AZ | CIDR | Purpose |
|---|---|---|---|
| `venuva-public-1a` | `me-south-1a` | `10.0.1.0/24` | Frontend, NAT GW, ALB |
| `venuva-public-1b` | `me-south-1b` | `10.0.2.0/24` | Frontend, ALB (secondary) |

**Steps for each public subnet:**
1. Go to **VPC → Subnets → Create Subnet**
2. Select **VPC:** `venuva-vpc`
3. Enter name, AZ, CIDR as above
4. After creation → select subnet → **Actions → Edit Subnet Settings** → enable **Auto-assign public IPv4 address**

#### Private Subnets (for backend EC2s and RDS)

| Name | AZ | CIDR | Purpose |
|---|---|---|---|
| `venuva-private-app-1a` | `me-south-1a` | `10.0.10.0/24` | Backend EC2 (all 5 services) |
| `venuva-private-app-1b` | `me-south-1b` | `10.0.11.0/24` | Backend EC2 (Auto Scaling) |
| `venuva-private-db-1a` | `me-south-1a` | `10.0.20.0/24` | RDS primary |
| `venuva-private-db-1b` | `me-south-1b` | `10.0.21.0/24` | RDS standby/replica |

> Do **NOT** enable auto-assign public IP for private subnets.

---

### Step 2.3 — Internet Gateway

1. Go to **VPC → Internet Gateways → Create Internet Gateway**
2. **Name:** `venuva-igw`
3. Click **Create**
4. Select it → **Actions → Attach to VPC** → select `venuva-vpc`

---

### Step 2.4 — Route Tables

#### Public Route Table
1. **VPC → Route Tables → Create Route Table**
   - **Name:** `venuva-public-rt`
   - **VPC:** `venuva-vpc`
2. Select it → **Routes tab → Edit routes → Add route:**
   - Destination: `0.0.0.0/0` | Target: `venuva-igw`
3. **Subnet associations tab → Edit subnet associations:**
   - Associate `venuva-public-1a` and `venuva-public-1b`

#### Private Route Table (App)
1. Create Route Table:
   - **Name:** `venuva-private-app-rt`
   - **VPC:** `venuva-vpc`
2. *(Add NAT Gateway route after Step 9)*
3. Associate `venuva-private-app-1a` and `venuva-private-app-1b`

#### Private Route Table (DB)
1. Create Route Table:
   - **Name:** `venuva-private-db-rt`
   - **VPC:** `venuva-vpc`
2. No internet route — DB subnet is fully isolated
3. Associate `venuva-private-db-1a` and `venuva-private-db-1b`

---

## 3. Security Groups

Go to **VPC → Security Groups → Create Security Group** for each:

### SG-1: `venuva-alb-sg` (Application Load Balancer)

| Type | Protocol | Port | Source | Description |
|---|---|---|---|---|
| Inbound | HTTPS | 443 | `0.0.0.0/0` | Public HTTPS |
| Inbound | HTTP | 80 | `0.0.0.0/0` | HTTP redirect |
| Outbound | All | All | `0.0.0.0/0` | Default |

### SG-2: `venuva-frontend-sg` (Frontend EC2)

| Type | Protocol | Port | Source | Description |
|---|---|---|---|---|
| Inbound | SSH | 22 | Your IP only | Admin access |
| Inbound | HTTP | 80 | `0.0.0.0/0` | Web traffic |
| Inbound | HTTPS | 443 | `0.0.0.0/0` | Secure web |
| Outbound | All | All | `0.0.0.0/0` | Default |

### SG-3: `venuva-backend-sg` (Backend EC2s — all 5 services)

| Type | Protocol | Port | Source | Description |
|---|---|---|---|---|
| Inbound | TCP | 8081 | `venuva-alb-sg` | Auth Service |
| Inbound | TCP | 8085 | `venuva-alb-sg` | Registration Service |
| Inbound | TCP | 8088 | `venuva-alb-sg` | Event Service |
| Inbound | TCP | 9090 | `venuva-alb-sg` | Notif Service |
| Inbound | TCP | 9099 | `venuva-alb-sg` | Payment Service |
| Inbound | TCP | 5672 | `venuva-backend-sg` | RabbitMQ (internal) |
| Inbound | TCP | 15672 | Your IP only | RabbitMQ console (admin) |
| Inbound | SSH | 22 | Your IP only | Admin access |
| Outbound | All | All | `0.0.0.0/0` | Default |

### SG-4: `venuva-rds-sg` (RDS MySQL)

| Type | Protocol | Port | Source | Description |
|---|---|---|---|---|
| Inbound | MySQL/Aurora | 3306 | `venuva-backend-sg` | Backend access only |
| Outbound | All | All | `0.0.0.0/0` | Default |

---

## 4. IAM Roles & Policies

Go to **IAM → Roles → Create Role** for each:

### Role 1: `venuva-ec2-backend-role`

**For:** Backend EC2 instances (all microservices)

**Steps:**
1. **IAM → Roles → Create Role**
2. Trusted entity: **AWS Service → EC2**
3. Attach policies:
   - `AmazonS3FullAccess` (for file storage)
   - `CloudWatchAgentServerPolicy` (for metrics/logs)
   - `AmazonSSMManagedInstanceCore` (for Systems Manager access without SSH)
4. **Role name:** `venuva-ec2-backend-role`

### Role 2: `venuva-ec2-frontend-role`

**For:** Frontend EC2

**Steps:**
1. Create Role → EC2
2. Attach policies:
   - `AmazonS3ReadOnlyAccess`
   - `CloudWatchAgentServerPolicy`
   - `AmazonSSMManagedInstanceCore`
3. **Role name:** `venuva-ec2-frontend-role`

### Role 3: `venuva-rds-monitoring-role`

**For:** RDS Enhanced Monitoring

1. Create Role → **AWS Service → RDS → RDS - Enhanced Monitoring**
2. Attach: `AmazonRDSEnhancedMonitoringRole`
3. **Role name:** `venuva-rds-monitoring-role`

---

## 5. RDS — MySQL Databases

### Step 5.1 — Create DB Subnet Group

1. Go to **RDS → Subnet Groups → Create DB Subnet Group**
2. Fill in:
   - **Name:** `venuva-db-subnet-group`
   - **VPC:** `venuva-vpc`
   - **Subnets:** add `venuva-private-db-1a` and `venuva-private-db-1b`
3. Click **Create**

### Step 5.2 — Create RDS MySQL Instance

> One RDS instance with **5 databases** (one per service). This is more cost-effective than 5 separate instances.

1. Go to **RDS → Databases → Create Database**
2. Settings:

| Field | Value |
|---|---|
| **Creation method** | Standard Create |
| **Engine** | MySQL 8.0 |
| **Template** | Production |
| **DB instance identifier** | `venuva-mysql` |
| **Master username** | `admin` |
| **Master password** | (create strong password, save it) |
| **DB instance class** | `db.t3.medium` (2 vCPU, 4 GB RAM) |
| **Multi-AZ** | Yes (creates standby in 1b) |
| **Storage type** | gp3 |
| **Allocated storage** | 100 GB |
| **Enable storage autoscaling** | Yes, max 500 GB |
| **VPC** | `venuva-vpc` |
| **DB subnet group** | `venuva-db-subnet-group` |
| **Public access** | No |
| **Security group** | `venuva-rds-sg` |
| **Initial database name** | `authdb` |
| **Backup retention** | 7 days |
| **Enable Enhanced Monitoring** | Yes → `venuva-rds-monitoring-role` |
| **Enable Performance Insights** | Yes |
| **Enable auto minor version upgrade** | Yes |

3. Click **Create Database** — wait ~10-15 minutes

### Step 5.3 — Create Remaining Databases

After the RDS instance is available, connect to it via the backend EC2 (using SSH tunnel or AWS Systems Manager) and run:

```sql
CREATE DATABASE registrationdb;
CREATE DATABASE event_service;
CREATE DATABASE notifdb;
CREATE DATABASE paymentdb;

-- Create app user with limited privileges
CREATE USER 'venuva_app'@'%' IDENTIFIED BY 'YourSecurePassword';
GRANT ALL PRIVILEGES ON authdb.* TO 'venuva_app'@'%';
GRANT ALL PRIVILEGES ON registrationdb.* TO 'venuva_app'@'%';
GRANT ALL PRIVILEGES ON event_service.* TO 'venuva_app'@'%';
GRANT ALL PRIVILEGES ON notifdb.* TO 'venuva_app'@'%';
GRANT ALL PRIVILEGES ON paymentdb.* TO 'venuva_app'@'%';
FLUSH PRIVILEGES;
```

> Note the RDS endpoint — you'll need it in `.env` files: `rds-endpoint.me-south-1.rds.amazonaws.com:3306`

---

## 6. S3 Bucket

Go to **S3 → Create Bucket**:

1. **Bucket name:** `venuva-assets-[your-account-id]` (must be globally unique)
2. **Region:** `me-south-1`
3. **Block all public access:** ON (keep default — backend accesses via IAM role)
4. **Versioning:** Enable
5. **Server-side encryption:** Enable (SSE-S3)
6. Click **Create Bucket**

### Create Folders

After creation, click the bucket → **Create folder:**
- `uploads/` — user-uploaded files
- `logs/` — application log archives
- `backups/` — database backup exports

### Bucket Policy

Go to **Permissions → Bucket Policy → Edit**, paste:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowBackendEC2Role",
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::YOUR_ACCOUNT_ID:role/venuva-ec2-backend-role"
      },
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::venuva-assets-YOUR_ACCOUNT_ID",
        "arn:aws:s3:::venuva-assets-YOUR_ACCOUNT_ID/*"
      ]
    }
  ]
}
```

> Replace `YOUR_ACCOUNT_ID` with your actual AWS account ID (found in top-right corner of console).

---

## 7. EC2 — Backend (Private Subnets)

> **Strategy:** One EC2 instance runs all 5 microservices + RabbitMQ as Docker containers. The Auto Scaling Group will launch identical instances from an AMI.

### Step 7.1 — Create Key Pair

1. Go to **EC2 → Key Pairs → Create Key Pair**
2. **Name:** `venuva-key`
3. **Type:** RSA
4. **Format:** `.pem`
5. Download and save securely

### Step 7.2 — Launch First Backend EC2

1. Go to **EC2 → Instances → Launch Instance**
2. Configure:

| Field | Value |
|---|---|
| **Name** | `venuva-backend-1` |
| **AMI** | Amazon Linux 2023 (free tier eligible, latest) |
| **Instance type** | `t3.xlarge` (4 vCPU, 16 GB — needed for 5 services + RabbitMQ) |
| **Key pair** | `venuva-key` |
| **VPC** | `venuva-vpc` |
| **Subnet** | `venuva-private-app-1a` |
| **Auto-assign public IP** | Disable |
| **Security group** | `venuva-backend-sg` |
| **IAM instance profile** | `venuva-ec2-backend-role` |
| **Root volume** | 30 GB gp3 |

3. **Advanced details → User data** — paste the following bootstrap script:

```bash
#!/bin/bash
# Update system
dnf update -y

# Install Docker
dnf install -y docker
systemctl enable docker
systemctl start docker
usermod -aG docker ec2-user

# Install Docker Compose
curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Install Java 21 (for running jars directly if needed)
dnf install -y java-21-amazon-corretto

# Install CloudWatch agent
dnf install -y amazon-cloudwatch-agent

# Install git (to pull code)
dnf install -y git

# Create app directory
mkdir -p /opt/venuva
chown ec2-user:ec2-user /opt/venuva

echo "Bootstrap complete" >> /var/log/venuva-bootstrap.log
```

4. Click **Launch Instance**

### Step 7.3 — Deploy Services on EC2

After the instance is running, connect via **Session Manager** (AWS Systems Manager):
1. **EC2 → Instances → select instance → Connect → Session Manager → Connect**

Then on the instance:

```bash
# Switch to ec2-user
sudo su - ec2-user
cd /opt/venuva

# Create environment file
cat > .env.prod << 'EOF'
SPRING_DATASOURCE_URL=jdbc:mysql://YOUR_RDS_ENDPOINT:3306
SPRING_DATASOURCE_USERNAME=venuva_app
SPRING_DATASOURCE_PASSWORD=YourSecurePassword
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
AUTH_SERVICE_URL=http://localhost:8081
EVENT_SERVICE_URL=http://localhost:8088
REGISTRATION_SERVICE_URL=http://localhost:8085
NOTIFICATION_SERVICE_URL=http://localhost:9090
PAYMENT_SERVICE_URL=http://localhost:9099
SPRING_MAIL_USERNAME=Waselny615@gmail.com
SPRING_MAIL_PASSWORD=uhnp sryl muzi bqiy
PAYMOB_API_KEY=...
PAYMOB_INTEGRATION_ID=5403815
PAYMOB_IFRAME_ID=980804
HMAC_SECRET_KEY=F7AE6EE1CCBE4AC58BF60ECA4ABA3AA6
EOF

# Create docker-compose.prod.yml
cat > docker-compose.prod.yml << 'EOF'
version: '3.8'
services:
  rabbitmq:
    image: rabbitmq:3-management
    container_name: rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    restart: always
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  auth-service:
    image: YOUR_ECR_OR_IMAGE/auth-service:latest
    container_name: auth-service
    ports:
      - "8081:8081"
    env_file: .env.prod
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SERVER_PORT: 8081
      SPRING_DATASOURCE_URL: jdbc:mysql://${SPRING_DATASOURCE_URL}/authdb?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false
    depends_on:
      rabbitmq:
        condition: service_healthy
    restart: always

  registration-service:
    image: YOUR_ECR_OR_IMAGE/registration-service:latest
    container_name: registration-service
    ports:
      - "8085:8085"
    env_file: .env.prod
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SERVER_PORT: 8085
      SPRING_DATASOURCE_URL: jdbc:mysql://${SPRING_DATASOURCE_URL}/registrationdb?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false
    depends_on:
      rabbitmq:
        condition: service_healthy
    restart: always

  event-service:
    image: YOUR_ECR_OR_IMAGE/event-service:latest
    container_name: event-service
    ports:
      - "8088:8088"
    env_file: .env.prod
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SERVER_PORT: 8088
      SPRING_DATASOURCE_URL: jdbc:mysql://${SPRING_DATASOURCE_URL}/event_service?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false
    depends_on:
      rabbitmq:
        condition: service_healthy
    restart: always

  notif-service:
    image: YOUR_ECR_OR_IMAGE/notif-service:latest
    container_name: notif-service
    ports:
      - "9090:9090"
    env_file: .env.prod
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SERVER_PORT: 9090
      SPRING_DATASOURCE_URL: jdbc:mysql://${SPRING_DATASOURCE_URL}/notifdb?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false
    depends_on:
      rabbitmq:
        condition: service_healthy
    restart: always

  paymentservice:
    image: YOUR_ECR_OR_IMAGE/paymentservice:latest
    container_name: paymentservice
    ports:
      - "9099:9099"
    env_file: .env.prod
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SERVER_PORT: 9099
      SPRING_DATASOURCE_URL: jdbc:mysql://${SPRING_DATASOURCE_URL}/paymentdb?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false
    depends_on:
      rabbitmq:
        condition: service_healthy
    restart: always
EOF

# Start all services
docker-compose -f docker-compose.prod.yml up -d
```

### Step 7.4 — Build Docker Images and Push to ECR

Before deploying, push your images to Amazon ECR:

1. Go to **ECR → Create Repository** for each service:
   - `venuva/auth-service`
   - `venuva/registration-service`
   - `venuva/event-service`
   - `venuva/notif-service`
   - `venuva/paymentservice`

2. For each repo, click **View push commands** and run them from your local machine where the code is built.

3. Update the `docker-compose.prod.yml` image fields with the ECR URLs (format: `ACCOUNT_ID.dkr.ecr.me-south-1.amazonaws.com/venuva/auth-service:latest`)

### Step 7.5 — Create AMI from Backend EC2

After all services are running and verified:

1. Go to **EC2 → Instances → select `venuva-backend-1`**
2. **Actions → Image and Templates → Create Image**
3. **Image name:** `venuva-backend-ami`
4. **No reboot:** unchecked (safer)
5. Click **Create Image** — wait ~5-10 minutes

---

## 8. Auto Scaling Group

### Step 8.1 — Create Launch Template

1. Go to **EC2 → Launch Templates → Create Launch Template**
2. Configure:

| Field | Value |
|---|---|
| **Name** | `venuva-backend-lt` |
| **AMI** | Select `venuva-backend-ami` (created in Step 7.5) |
| **Instance type** | `t3.xlarge` |
| **Key pair** | `venuva-key` |
| **Security group** | `venuva-backend-sg` |
| **IAM instance profile** | `venuva-ec2-backend-role` |

3. **Advanced details → User data:**
```bash
#!/bin/bash
cd /opt/venuva
docker-compose -f docker-compose.prod.yml up -d
```

4. Click **Create Launch Template**

### Step 8.2 — Create Auto Scaling Group

1. Go to **EC2 → Auto Scaling Groups → Create Auto Scaling Group**
2. Configure:

**Step 1 — Choose launch template:**
- **Name:** `venuva-backend-asg`
- **Launch template:** `venuva-backend-lt`

**Step 2 — Choose instance launch options:**
- **VPC:** `venuva-vpc`
- **Availability Zones and subnets:** `venuva-private-app-1a`, `venuva-private-app-1b`

**Step 3 — Configure advanced options:**
- **Load balancing:** Attach to existing load balancer (configure after Step 10)
- **Health check type:** ELB
- **Health check grace period:** 300 seconds (5 min for services to start)

**Step 4 — Configure group size and scaling:**
- **Desired capacity:** 2
- **Minimum capacity:** 1
- **Maximum capacity:** 4

**Target Tracking Scaling Policy:**
- **Metric type:** Average CPU Utilization
- **Target value:** 70%

**Step 5 — Add notifications:**
- Create SNS topic: `venuva-asg-alerts`
- Add your email to receive scale-in/out notifications

**Step 6 — Add tags:**
- Key: `Name` | Value: `venuva-backend`
- Key: `Environment` | Value: `production`

3. Click **Create Auto Scaling Group**

---

## 9. NAT Gateway

> Required so private-subnet EC2s can pull Docker images from ECR and connect to the internet for external APIs (PayMob, Gmail SMTP).

1. Go to **VPC → NAT Gateways → Create NAT Gateway**
2. Configure:

| Field | Value |
|---|---|
| **Name** | `venuva-nat-gw` |
| **Subnet** | `venuva-public-1a` (must be PUBLIC) |
| **Connectivity type** | Public |
| **Elastic IP** | Click **Allocate Elastic IP** |

3. Click **Create NAT Gateway** — wait ~2 minutes

### Add NAT Route to Private App Route Table

1. Go to **VPC → Route Tables → select `venuva-private-app-rt`**
2. **Routes → Edit routes → Add route:**
   - Destination: `0.0.0.0/0`
   - Target: `venuva-nat-gw`
3. **Save changes**

> Do NOT add a NAT route to `venuva-private-db-rt` — database subnets stay fully isolated.

---

## 10. Load Balancer (ALB)

### Step 10.1 — Create Application Load Balancer

1. Go to **EC2 → Load Balancers → Create Load Balancer → Application Load Balancer**
2. Configure:

| Field | Value |
|---|---|
| **Name** | `venuva-alb` |
| **Scheme** | Internet-facing |
| **IP address type** | IPv4 |
| **VPC** | `venuva-vpc` |
| **Mappings** | Select `venuva-public-1a` and `venuva-public-1b` |
| **Security groups** | `venuva-alb-sg` |

### Step 10.2 — Create Target Groups

Create **5 target groups** (one per service):

For each, go to **EC2 → Target Groups → Create Target Group:**

| Target Group | Port | Health Check Path |
|---|---|---|
| `venuva-tg-auth` | 8081 | `/api/auth/check-email?email=test@test.com` |
| `venuva-tg-registration` | 8085 | `/actuator/health` or `/api/registrations/getNumberOfRegesters` |
| `venuva-tg-event` | 8088 | `/api/events` |
| `venuva-tg-notif` | 9090 | `/actuator/health` |
| `venuva-tg-payment` | 9099 | `/actuator/health` |

For each target group:
- **Target type:** Instances
- **VPC:** `venuva-vpc`
- **Health check protocol:** HTTP
- **Healthy threshold:** 2 | **Unhealthy threshold:** 3 | **Interval:** 30s
- Register the backend EC2 instance(s) and the Auto Scaling Group

### Step 10.3 — Add Listeners and Rules

**Listener 1: HTTP:80** → Redirect to HTTPS:443

**Listener 2: HTTPS:443**
- **Certificate:** Upload or request via ACM (AWS Certificate Manager)
- Add rules for path-based routing:

| Priority | Path Pattern | Forward To |
|---|---|---|
| 1 | `/api/auth/*` | `venuva-tg-auth` |
| 2 | `/api/registrations/*` | `venuva-tg-registration` |
| 3 | `/api/events/*` | `venuva-tg-event` |
| 4 | `/api/categories/*` | `venuva-tg-event` |
| 5 | `/api/notifications/*` | `venuva-tg-notif` |
| 6 | `/api/Paymob/*` | `venuva-tg-payment` |

> Go to **Listeners → HTTPS:443 → View/edit rules → Add rule** for each path.

### Step 10.4 — Add SSL Certificate (ACM)

1. Go to **Certificate Manager → Request Certificate → Request a public certificate**
2. **Domain name:** `api.yourdomain.com` (and `*.yourdomain.com`)
3. **Validation method:** DNS validation
4. Add the CNAME records to your DNS provider
5. After validated, attach to the ALB HTTPS listener

---

## 11. API Gateway

> API Gateway sits in front of the ALB, providing rate limiting, authentication at edge, and a clean API URL.

1. Go to **API Gateway → Create API → REST API → Build**
2. Configure:

| Field | Value |
|---|---|
| **Protocol** | REST |
| **Create new API** | New API |
| **API name** | `venuva-api` |
| **Endpoint type** | Regional |

### Create Resources and Methods

For each service, create a resource:

1. **Resources → Create Resource:**
   - Resource path: `{proxy+}` (greedy path variable)
   - Enable CORS: Yes

2. **Create Method → ANY** on each resource:
   - Integration type: **HTTP Proxy**
   - HTTP method: ANY
   - Endpoint URL: `http://venuva-alb-XXXXXX.me-south-1.elb.amazonaws.com/{proxy}`
   - Use proxy integration: Yes

### Enable Usage Plans (Rate Limiting)

1. **API Gateway → Usage Plans → Create**
   - **Name:** `venuva-standard`
   - **Throttling:** Rate: `1000` requests/sec, Burst: `500`
   - **Quota:** `100000` requests/day

### Deploy API

1. **Actions → Deploy API**
2. **Stage:** Create new stage → `prod`
3. Note the Invoke URL: `https://XXXXXX.execute-api.me-south-1.amazonaws.com/prod`

---

## 12. EC2 — Frontend (Public Subnet)

1. Go to **EC2 → Launch Instance**
2. Configure:

| Field | Value |
|---|---|
| **Name** | `venuva-frontend` |
| **AMI** | Amazon Linux 2023 |
| **Instance type** | `t3.small` (2 vCPU, 2 GB — sufficient for React/Nginx) |
| **Key pair** | `venuva-key` |
| **VPC** | `venuva-vpc` |
| **Subnet** | `venuva-public-1a` |
| **Auto-assign public IP** | Enable |
| **Security group** | `venuva-frontend-sg` |
| **IAM instance profile** | `venuva-ec2-frontend-role` |
| **Root volume** | 20 GB gp3 |

3. **User data:**
```bash
#!/bin/bash
dnf update -y
dnf install -y nginx git nodejs npm

# Enable and start nginx
systemctl enable nginx
systemctl start nginx

echo "Frontend EC2 ready" >> /var/log/venuva-bootstrap.log
```

4. After launch, connect and deploy your React/frontend build, configuring the API base URL to point to the API Gateway invoke URL.

---

## 13. CloudWatch — Monitoring & Alerts

### Step 13.1 — Install CloudWatch Agent on EC2s

Connect to each EC2 via Session Manager and run:

```bash
# Download and install the agent
sudo dnf install -y amazon-cloudwatch-agent

# Create the configuration file
sudo cat > /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json << 'EOF'
{
  "logs": {
    "logs_collected": {
      "files": {
        "collect_list": [
          {
            "file_path": "/opt/venuva/Logs/*.log",
            "log_group_name": "/venuva/application",
            "log_stream_name": "{instance_id}-app-logs"
          },
          {
            "file_path": "/var/log/venuva-bootstrap.log",
            "log_group_name": "/venuva/bootstrap",
            "log_stream_name": "{instance_id}-bootstrap"
          }
        ]
      }
    }
  },
  "metrics": {
    "namespace": "Venuva/EC2",
    "metrics_collected": {
      "cpu": {
        "measurement": ["cpu_usage_idle", "cpu_usage_user"],
        "metrics_collection_interval": 60
      },
      "mem": {
        "measurement": ["mem_used_percent"],
        "metrics_collection_interval": 60
      },
      "disk": {
        "measurement": ["disk_used_percent"],
        "metrics_collection_interval": 300
      }
    }
  }
}
EOF

# Start the agent
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
  -a fetch-config \
  -m ec2 \
  -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json \
  -s
```

### Step 13.2 — Create Log Groups

Go to **CloudWatch → Logs → Log Groups → Create Log Group:**

| Log Group | Retention |
|---|---|
| `/venuva/application` | 30 days |
| `/venuva/bootstrap` | 7 days |
| `/venuva/rds` | 30 days |

### Step 13.3 — Create Alarms

Go to **CloudWatch → Alarms → Create Alarm** for each:

#### Alarm 1: High CPU (Backend)
- **Metric:** EC2 → Per-Instance Metrics → `CPUUtilization`
- **Condition:** Greater than 80% for 2 consecutive periods (5 min)
- **Action:** Send notification to SNS topic `venuva-alerts`

#### Alarm 2: RDS CPU
- **Metric:** RDS → Per-Database Metrics → `CPUUtilization`
- **Condition:** Greater than 75% for 2 periods
- **Action:** SNS → `venuva-alerts`

#### Alarm 3: RDS Free Storage Space
- **Metric:** RDS → `FreeStorageSpace`
- **Condition:** Less than 10 GB
- **Action:** SNS → `venuva-alerts`

#### Alarm 4: ALB 5XX Errors
- **Metric:** ApplicationELB → Per-AppELB → `HTTPCode_ELB_5XX_Count`
- **Condition:** Greater than 10 in 5 minutes
- **Action:** SNS → `venuva-alerts`

#### Alarm 5: Auto Scaling Group Health
- **Metric:** AutoScaling → `GroupInServiceInstances`
- **Condition:** Less than 1
- **Action:** SNS → `venuva-alerts`

### Step 13.4 — Create SNS Topic for Alerts

1. Go to **SNS → Topics → Create Topic**
2. **Type:** Standard | **Name:** `venuva-alerts`
3. **Subscriptions → Create Subscription:**
   - Protocol: Email | Endpoint: your-email@example.com
4. Confirm the subscription from your email

### Step 13.5 — Create CloudWatch Dashboard

1. Go to **CloudWatch → Dashboards → Create Dashboard**
2. **Name:** `VenuvaProductionDashboard`
3. Add widgets:
   - **Line chart:** EC2 CPU Utilization (all backend instances)
   - **Line chart:** RDS CPU + Free Storage
   - **Number:** ALB Request Count
   - **Number:** ALB 5XX Error Rate
   - **Line chart:** ASG In-Service Instance Count
   - **Log insights widget:** query `/venuva/application` for ERROR lines

---

## 14. Deployment Order Checklist

Follow this exact order to avoid dependency issues:

```
 PHASE 1 — Foundation
[ ] 1.  Set region to me-south-1
[ ] 2.  Create VPC (venuva-vpc)
[ ] 3.  Create 6 Subnets (2 public, 2 private-app, 2 private-db)
[ ] 4.  Create Internet Gateway → attach to VPC
[ ] 5.  Create Route Tables (public, private-app, private-db)
[ ] 6.  Create Security Groups (alb-sg, frontend-sg, backend-sg, rds-sg)
[ ] 7.  Create IAM Roles (ec2-backend-role, ec2-frontend-role, rds-monitoring-role)

 PHASE 2 — Storage & Data
[ ] 8.  Create S3 Bucket + folder structure + bucket policy
[ ] 9.  Create DB Subnet Group
[ ] 10. Create RDS MySQL instance (Multi-AZ, private subnets)
[ ] 11. Connect to RDS → create 4 additional databases + venuva_app user

 PHASE 3 — NAT Gateway
[ ] 12. Allocate Elastic IP
[ ] 13. Create NAT Gateway in public-1a
[ ] 14. Add NAT route to private-app route table

 PHASE 4 — Backend EC2 & Services
[ ] 15. Create Key Pair
[ ] 16. Create ECR repositories (5 repos)
[ ] 17. Build Docker images locally → push to ECR
[ ] 18. Launch backend EC2 in private-app-1a
[ ] 19. Connect via Session Manager → deploy docker-compose → verify all services running
[ ] 20. Create AMI from backend EC2

 PHASE 5 — Auto Scaling
[ ] 21. Create Launch Template from AMI
[ ] 22. Create Target Groups (5 groups)
[ ] 23. Create Auto Scaling Group (desired=2, min=1, max=4)

 PHASE 6 — Load Balancer
[ ] 24. Request SSL certificate in ACM
[ ] 25. Create Application Load Balancer (internet-facing, public subnets)
[ ] 26. Add HTTPS listener → configure path-based routing rules
[ ] 27. Attach ASG to ALB target groups
[ ] 28. Validate health checks are passing

 PHASE 7 — API Gateway
[ ] 29. Create REST API
[ ] 30. Create proxy resource → ANY method → ALB endpoint
[ ] 31. Create Usage Plan with throttling
[ ] 32. Deploy to prod stage

 PHASE 8 — Frontend
[ ] 33. Launch frontend EC2 (public subnet, public IP)
[ ] 34. Deploy React app with API base URL = API Gateway invoke URL
[ ] 35. Configure Nginx to serve the app on port 80/443

 PHASE 9 — Monitoring
[ ] 36. Install CloudWatch agent on all EC2s
[ ] 37. Create Log Groups
[ ] 38. Create SNS Topic → add email subscription
[ ] 39. Create 5 CloudWatch Alarms
[ ] 40. Create CloudWatch Dashboard
```

---

## 15. Architecture Diagram (Text)

```
INTERNET
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│  AWS Region: me-south-1 (Bahrain)                               │
│                                                                 │
│  ┌── VPC: venuva-vpc (10.0.0.0/16) ───────────────────────┐   │
│  │                                                          │   │
│  │  ┌─── PUBLIC SUBNETS ──────────────────────────────┐   │   │
│  │  │  10.0.1.0/24 (AZ-a) │ 10.0.2.0/24 (AZ-b)      │   │   │
│  │  │                                                  │   │   │
│  │  │  [Internet Gateway]                              │   │   │
│  │  │       │                                          │   │   │
│  │  │  [Application Load Balancer venuva-alb]         │   │   │
│  │  │       │                                          │   │   │
│  │  │  [NAT Gateway]  [Frontend EC2 t3.small]         │   │   │
│  │  └──────────────────────────────────────────────────┘   │   │
│  │           │                          │                   │   │
│  │           ▼                          ▼                   │   │
│  │  ┌─── PRIVATE APP SUBNETS ───────────────────────────┐  │   │
│  │  │  10.0.10.0/24 (AZ-a) │ 10.0.11.0/24 (AZ-b)      │  │   │
│  │  │                                                    │  │   │
│  │  │  ┌───── Auto Scaling Group ──────────────────┐    │  │   │
│  │  │  │  Backend EC2 (t3.xlarge) [AZ-a]           │    │  │   │
│  │  │  │  Backend EC2 (t3.xlarge) [AZ-b]           │    │  │   │
│  │  │  │                                            │    │  │   │
│  │  │  │  Each EC2 runs (Docker):                   │    │  │   │
│  │  │  │  ├─ RabbitMQ :5672 / :15672               │    │  │   │
│  │  │  │  ├─ Auth Service :8081                     │    │  │   │
│  │  │  │  ├─ Event Service :8088                    │    │  │   │
│  │  │  │  ├─ Registration Service :8085             │    │  │   │
│  │  │  │  ├─ Notif Service :9090                    │    │  │   │
│  │  │  │  └─ Payment Service :9099                  │    │  │   │
│  │  │  └────────────────────────────────────────────┘    │  │   │
│  │  └────────────────────────────────────────────────────┘  │   │
│  │           │                                               │   │
│  │           ▼                                               │   │
│  │  ┌─── PRIVATE DB SUBNETS ────────────────────────────┐   │   │
│  │  │  10.0.20.0/24 (AZ-a) │ 10.0.21.0/24 (AZ-b)      │   │   │
│  │  │                                                    │   │   │
│  │  │  [RDS MySQL db.t3.medium - Multi-AZ]              │   │   │
│  │  │  Databases: authdb, registrationdb,               │   │   │
│  │  │             event_service, notifdb, paymentdb     │   │   │
│  │  └────────────────────────────────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Global Services:                                               │
│  [API Gateway] → [ALB]                                         │
│  [S3: venuva-assets] (logs, uploads, backups)                  │
│  [CloudWatch] (metrics, logs, alarms, dashboard)               │
│  [ECR] (Docker image registry)                                  │
│  [IAM] (roles & policies)                                       │
│  [ACM] (SSL certificates)                                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## Quick Reference: Key Values

| Resource | Name/Value |
|---|---|
| Region | `me-south-1` |
| VPC CIDR | `10.0.0.0/16` |
| Public Subnets | `10.0.1.0/24`, `10.0.2.0/24` |
| Private App Subnets | `10.0.10.0/24`, `10.0.11.0/24` |
| Private DB Subnets | `10.0.20.0/24`, `10.0.21.0/24` |
| Backend Instance Type | `t3.xlarge` |
| Frontend Instance Type | `t3.small` |
| RDS Instance Class | `db.t3.medium` |
| ASG Desired/Min/Max | `2 / 1 / 4` |
| ALB Type | Internet-facing, Application |
| API Gateway Stage | `prod` |
| CloudWatch Log Retention | 30 days (app), 7 days (bootstrap) |

---

*Generated for Venuva Microservices Platform — May 2026*

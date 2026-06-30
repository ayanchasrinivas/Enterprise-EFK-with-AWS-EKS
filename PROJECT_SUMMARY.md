# OpsBrain: Complete Incident Management Platform - Project Summary

## 🎯 Project Overview

**OpsBrain** is a production-grade DevOps Incident Management & Response Platform combining:
- **ELK/EFK Stack**: Log aggregation and visualization
- **AWS EKS**: Kubernetes orchestration on AWS
- **Microservices**: 5 independent services with Kafka messaging
- **AI Analysis**: AWS Bedrock Claude for root cause analysis
- **Multi-tenant Dashboar**: Next.js frontend for incident management

---

## 📦 Complete Deliverables

### Backend Services (5 Microservices)

#### 1. **Incident Service** (`services/incident-service/`)
- Deduplicates analyses into incidents
- Persists to PostgreSQL with Flyway migrations
- REST API for CRUD operations
- Technologies: Spring Boot 3.2, PostgreSQL, Flyway
- Ports: 8080
- Features:
  - Incident deduplication using Levenshtein distance
  - Status tracking (OPEN, ACKNOWLEDGED, MITIGATED, RESOLVED)
  - Severity classification
  - Swagger UI + Prometheus metrics

#### 2. **On-Call Service** (`services/on-call-service/`)
- Manages on-call schedules and rotations
- Team member management
- Escalation policies by severity
- Quartz scheduling for rotation automation
- Technologies: Spring Boot, PostgreSQL, Quartz Scheduler
- Ports: 8081
- Features:
  - Weekly/bi-weekly/monthly rotation types
  - Escalation levels (L1-L4)
  - Current on-call lookup by service
  - Kafka producer for notifications

#### 3. **Notification Service** (`services/notification-service/`)
- Sends incident notifications to Slack & Teams
- Structured message formatting (blocks/cards)
- Failure tracking and retry mechanism (Resilience4j)
- Notification logging for audit trail
- Technologies: Spring Boot, WebFlux, Resilience4j
- Ports: 8082
- Features:
  - Slack webhook integration
  - Teams adaptive cards
  - Retry logic with exponential backoff
  - Template support for custom messages

#### 4. **Postmortem Service** (`services/postmortem-service/`)
- Generates incident postmortems using AWS Bedrock Claude
- PDF and Markdown output formats
- Automated RCA (Root Cause Analysis)
- Technologies: Spring Boot, AWS Bedrock SDK, iText PDF
- Ports: 8083
- Features:
  - AI-powered postmortem generation
  - PDF + Markdown export
  - Section extraction (timeline, RCA, lessons learned)
  - Bedrock Claude integration

#### 5. **Alert Ingestion Service** (from previous work)
- Entry point for alerts from external sources
- Webhook receivers for Prometheus, Grafana, CloudWatch
- Kafka producer for alert distribution

### Frontend

#### **Next.js Dashboard** (`frontend/`)
- Modern, responsive incident management UI
- Technologies: Next.js 14, React 18, Tailwind CSS, Recharts
- Ports: 3000
- Pages:
  - **Dashboard**: Real-time stats (open incidents, resolved today, avg resolution time, critical alerts)
  - **Incidents**: List with filtering, detailed view, status updates
  - **On-Call**: Schedule visibility, team member contact info
  - **Notifications**: History of sent notifications
  - **Postmortems**: Generated postmortem documents with download

### Infrastructure & Deployment

#### Kubernetes Manifests
- **incident-service-deployment.yaml**: Deployment, Service, HPA (2-5 replicas), PDB
- **on-call-service-deployment.yaml**: Deployment, Service, HPA (2-5 replicas), PDB
- **notification-service-deployment.yaml**: Deployment, Service, HPA (2-5 replicas), PDB
- **postmortem-service-deployment.yaml**: Deployment, Service, HPA (2-5 replicas), PDB
- **frontend-deployment.yaml**: Deployment, Service, HPA (3-10 replicas), NetworkPolicy
- **ingress.yaml**: NGINX Ingress with SSL/TLS support
- **namespace.yaml**: Kubernetes namespace configuration

#### ArgoCD Applications
- `argocd/applications/incident-service-app.yaml`
- `argocd/applications/on-call-service-app.yaml`
- `argocd/applications/notification-service-app.yaml`
- `argocd/applications/postmortem-service-app.yaml`
- `argocd/applications/frontend-app.yaml`

#### Docker Builds
- Multi-stage Dockerfiles for all services and frontend
- Alpine Linux for minimal image size
- Health checks configured
- Security best practices (non-root user, read-only FS)

### CI/CD Pipeline

#### Jenkinsfile (Comprehensive Pipeline)
```
Stages:
1. Checkout: Code retrieval
2. SonarQube Analysis: SAST scanning
3. Build & Push Images: Multi-parallel Docker builds to ECR
4. Security Scanning: Trivy for vulnerabilities, SBOM generation
5. Deploy to EKS: ArgoCD sync
6. Smoke Tests: Health checks
7. Post-Deployment Monitoring: Pod health verification
```

### Supporting Files

#### Local Development
- **docker-compose.yml**: Full stack locally (all services + databases + Kafka + Elasticsearch)
- **.gitignore**: Standard Java/Node.js ignores

#### Deployment Documentation
- **DEPLOYMENT_PLAN.md**: 6-stage deployment with detailed runbooks
- **PROJECT_SUMMARY.md**: This file

---

## 🏗️ Architecture & Data Flow

```
External Alerts
    │
    ├─ Prometheus AlertManager
    ├─ Grafana Webhooks
    ├─ CloudWatch Events
    └─ Kubernetes Events
    │
    ▼
[alert-ingestion-service] (Entry point)
    │ Kafka: raw-alerts
    ▼
[context-collector-service] (Enriches with logs/metrics)
    │ Kafka: context-bundles
    ▼
[ai-analysis-service] (AWS Bedrock Claude)
    │ Kafka: analysis-output
    ▼
[incident-service] (Deduplication + CRUD)
    │ Kafka: incident-assigned
    ├─────────┬──────────────┐
    │         │              │
    ▼         ▼              ▼
[on-call] [notification] [postmortem]
    │         │              │
    └─────────┼──────────────┘
              │
              ▼
        [PostgreSQL] (4 databases)
        [Redis] (cache)
        [Elasticsearch] (logs)
              │
              ▼
        [Next.js Frontend]
```

---

## 🚀 Getting Started

### Prerequisites
- Docker & Docker Compose
- Java 17+
- Node.js 20+
- Python 3.9+ (for AWS CLI)
- AWS Account with EKS cluster
- kubectl configured

### Local Development (Docker Compose)

```bash
# Clone repository
git clone https://github.com/ayanchasrinivas/Enterprise-EFK-with-AWS-EKS
cd Enterprise-EFK-with-AWS-EKS

# Start entire stack
docker-compose up -d

# Verify services
docker-compose ps

# Access services
# Frontend: http://localhost:3000
# Incident API: http://localhost:8080/swagger-ui.html
# On-Call API: http://localhost:8081/swagger-ui.html
# Notification API: http://localhost:8082/swagger-ui.html
# Postmortem API: http://localhost:8083/swagger-ui.html
# Elasticsearch: http://localhost:9200
# Kibana: http://localhost:5601
```

### Production Deployment

See **DEPLOYMENT_PLAN.md** for comprehensive 6-stage deployment guide:
1. Environment Preparation (Days 1-2)
2. Build & Push Services (Days 2-3)
3. Deploy to EKS (Days 3-4)
4. Integration Testing (Days 4-5)
5. Production Hardening (Days 5-6)
6. Go Live (Day 7)

---

## 📊 Key Features

### Incident Management
✅ Intelligent deduplication (60-minute window, similarity matching)
✅ Multi-severity levels (CRITICAL, HIGH, MEDIUM, LOW, INFO)
✅ Status workflow (OPEN → ACKNOWLEDGED → MITIGATED → RESOLVED)
✅ Root cause analysis via AI
✅ Remediation steps generation

### On-Call Management
✅ Flexible rotation schedules (weekly, bi-weekly, monthly)
✅ Multi-level escalation policies
✅ Team member contact management
✅ Current on-call lookup by service

### Notifications
✅ Slack integration (blocks formatting)
✅ Teams integration (adaptive cards)
✅ Retry mechanism with exponential backoff
✅ Notification audit trail
✅ Configurable templates

### Postmortems
✅ AI-generated postmortems (AWS Bedrock Claude)
✅ Automated section extraction (timeline, RCA, lessons)
✅ PDF + Markdown export
✅ Storage in S3 with audit trail

### Dashboard
✅ Real-time statistics
✅ Incident visualization
✅ On-call schedule display
✅ Notification history
✅ Postmortem access

---

## 💾 Database Schema

### Incident Service (PostgreSQL)
- `incidents`: Incident records with dedup count
- `incident_analyses`: Audit trail of linked analyses

### On-Call Service (PostgreSQL)
- `teams`: Team definitions
- `team_members`: Team member profiles
- `on_call_schedules`: Schedule definitions
- `on_call_rotations`: Rotation assignments
- `escalation_policies`: Escalation rules
- `on_call_assignments`: Incident assignments (with Quartz tables)

### Notification Service (PostgreSQL)
- `notification_logs`: Sent notifications with status
- `notification_templates`: Message templates
- `notification_channels`: Slack/Teams webhook configs

### Postmortem Service (PostgreSQL)
- `postmortems`: Generated postmortem records
- `postmortem_attachments`: PDF/MD files with storage location

---

## 🔐 Security

### Kubernetes Security
- ✅ Pod Security Policies (restricted)
- ✅ Network Policies (namespace isolation)
- ✅ RBAC configured for each service
- ✅ Non-root containers (UID 1000)
- ✅ Read-only root filesystems
- ✅ Security context: DROP all capabilities

### Application Security
- ✅ Input validation on all APIs
- ✅ Error handling without info leakage
- ✅ Secrets managed via Kubernetes secrets
- ✅ Database credentials encrypted
- ✅ Audit logging for critical operations

### Infrastructure Security
- ✅ Private subnets for services
- ✅ Security groups restricting traffic
- ✅ SSL/TLS with Let's Encrypt
- ✅ AWS Bedrock IAM role-based access
- ✅ S3 bucket encryption

---

## 📈 Performance & Scaling

### Auto-Scaling
- **Incident Service**: 2-5 replicas (CPU 70%, Memory 80%)
- **On-Call Service**: 2-5 replicas (CPU 70%, Memory 80%)
- **Notification Service**: 2-5 replicas (CPU 70%, Memory 80%)
- **Postmortem Service**: 2-5 replicas (CPU 70%, Memory 80%)
- **Frontend**: 3-10 replicas (CPU 70%, Memory 80%)

### Resource Allocation
- **Services**: 250m CPU request, 512Mi memory request | 500m CPU limit, 1Gi memory limit
- **Frontend**: 100m CPU request, 256Mi memory request | 500m CPU limit, 512Mi memory limit

### Pod Disruption Budgets
- All services: `minAvailable=1` (at least one pod running)

---

## 📚 Documentation

| Document | Purpose |
|----------|---------|
| [DEPLOYMENT_PLAN.md](DEPLOYMENT_PLAN.md) | 6-stage production deployment guide |
| [services/incident-service/README.md](services/incident-service/README.md) | Incident service details |
| [services/on-call-service/README.md](services/on-call-service/README.md) | On-call service details |
| [services/notification-service/README.md](services/notification-service/README.md) | Notification service details |
| [services/postmortem-service/README.md](services/postmortem-service/README.md) | Postmortem service details |
| [frontend/README.md](frontend/README.md) | Frontend setup & customization |

---

## 🛠️ Technology Stack

### Backend
- **Framework**: Spring Boot 3.2.5
- **Language**: Java 17
- **Database**: PostgreSQL 15
- **Messaging**: Apache Kafka (Strimzi)
- **Cache**: Redis
- **Job Scheduling**: Quartz Scheduler
- **Logging**: ELK Stack (Elasticsearch, Kibana, Fluent-bit)
- **Monitoring**: Prometheus + Grafana
- **AI/ML**: AWS Bedrock (Claude 3)

### Frontend
- **Framework**: Next.js 14
- **UI Library**: React 18
- **Styling**: Tailwind CSS
- **Charting**: Recharts
- **State Management**: Zustand
- **HTTP Client**: Axios, SWR

### Infrastructure
- **Container Orchestration**: AWS EKS (Kubernetes)
- **Container Runtime**: Docker
- **CI/CD**: Jenkins + ArgoCD
- **Code Analysis**: SonarQube
- **Security Scanning**: Trivy
- **IaC**: Terraform (existing)
- **Service Mesh** (optional): Istio/Linkerd ready

---

## 📋 Deployment Checklist

### Pre-Deployment
- [ ] AWS Account with EKS cluster
- [ ] RDS PostgreSQL instances created
- [ ] Kafka cluster deployed
- [ ] Redis deployed
- [ ] S3 bucket for postmortems
- [ ] Docker registry (ECR) access
- [ ] Secrets configured in Kubernetes
- [ ] ArgoCD installed on cluster

### Deployment
- [ ] Build all services locally
- [ ] Push Docker images to ECR
- [ ] Run SonarQube analysis
- [ ] Deploy via ArgoCD
- [ ] Verify pod health
- [ ] Configure Ingress + SSL
- [ ] Run smoke tests

### Post-Deployment
- [ ] Monitor metrics (P95 latency, error rate)
- [ ] Test incident flow (alert → incident → notification)
- [ ] Verify dashboard accessibility
- [ ] Check log aggregation
- [ ] Validate backups

---

## 🔄 Next Steps

1. **Immediate (Week 1)**:
   - [ ] Follow DEPLOYMENT_PLAN.md stages 1-3
   - [ ] Deploy all services to EKS
   - [ ] Configure on-call schedules

2. **Short-term (Week 2-3)**:
   - [ ] Integrate with existing Prometheus/Grafana
   - [ ] Configure Slack/Teams webhooks
   - [ ] Set up automated backups
   - [ ] Train team on dashboard

3. **Medium-term (Month 2)**:
   - [ ] Add additional alert sources
   - [ ] Customize AI prompts for better RCA
   - [ ] Implement additional escalation strategies
   - [ ] Build custom Grafana dashboards

4. **Long-term (Month 3+)**:
   - [ ] Multi-region deployment
   - [ ] Disaster recovery testing
   - [ ] Performance optimization
   - [ ] Community feedback & iterations

---

## 📞 Support

For issues, questions, or improvements:
- Create GitHub issue: https://github.com/ayanchasrinivas/Enterprise-EFK-with-AWS-EKS/issues
- Review service-specific READMEs
- Check DEPLOYMENT_PLAN.md troubleshooting section

---

## 📄 License

This project is provided as-is for enterprise use.

---

**Project Status**: ✅ **READY FOR PRODUCTION DEPLOYMENT**

Total Components:
- 5 Microservices
- 1 Frontend Dashboard
- 4 PostgreSQL Databases
- 1 Kafka Cluster
- 1 Redis Instance
- 1 ELK Stack
- 1 CI/CD Pipeline
- Complete IaC with Terraform

**Estimated Deployment Time**: 5-7 days following DEPLOYMENT_PLAN.md

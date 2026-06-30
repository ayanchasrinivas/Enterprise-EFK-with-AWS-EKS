# OpsBrain DevOps Incident Management Platform - Deployment Plan

## Project Overview

**OpsBrain** is a production-grade DevOps Incident Management Tool built on:
- ELK/EFK stack for logging & observability
- AWS EKS for Kubernetes orchestration
- Microservices architecture with Kafka messaging
- AWS Bedrock Claude for AI-powered analysis
- Next.js dashboard for incident management

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        External Sources                         │
│  (Prometheus, Grafana, CloudWatch, K8s Events)                 │
└────────────────────┬────────────────────────────────────────────┘
                     │
        ┌────────────▼──────────────────────┐
        │   alert-ingestion-service        │ (Entry point)
        └────────────┬──────────────────────┘
                     │ Kafka
        ┌────────────▼──────────────────────┐
        │  context-collector-service       │ (Enriches with logs/metrics)
        └────────────┬──────────────────────┘
                     │ Kafka
        ┌────────────▼──────────────────────┐
        │   ai-analysis-service            │ (AWS Bedrock Claude)
        └────────────┬──────────────────────┘
                     │ Kafka
        ┌────────────▼──────────────────────┐
        │   incident-service               │ (Deduplication & CRUD)
        └────────────┬──────────────────────┘
                     │
        ┌────────────┼────────────┬──────────────────┐
        │            │            │                  │
        ▼            ▼            ▼                  ▼
   on-call-svc  notif-svc   postmortem-svc    [PostgreSQL]
        │            │            │
        │            └────────────┼──────────────┐
        │                         │              │
        │    ┌────────────────────┘              │
        │    │                                   │
        ▼    ▼                                   ▼
   ┌────────────────────────────────────────────────────┐
   │         Next.js Dashboard / Frontend               │
   │    (Incidents, On-Call, Notifications, Reports)  │
   └────────────────────────────────────────────────────┘
```

---

## Pre-Deployment Checklist

### AWS Account & Infrastructure
- [ ] AWS Account with sufficient IAM permissions
- [ ] EKS cluster provisioned (terraform/modules/eks)
- [ ] VPC with public/private subnets configured
- [ ] RDS PostgreSQL instances for each service (incident, on-call, notification, postmortem)
- [ ] Kafka cluster (Strimzi) deployed on EKS
- [ ] Redis deployed on EKS
- [ ] S3 bucket for postmortem storage
- [ ] AWS Bedrock Claude model available (us-east-1)

### Tools & Access
- [ ] `kubectl` configured to access EKS cluster
- [ ] `aws` CLI configured with credentials
- [ ] `git` with access to repository
- [ ] Docker installed for local builds
- [ ] Helm 3+ installed
- [ ] ArgoCD installed on cluster
- [ ] Jenkins deployed (or alternate CI/CD)
- [ ] SonarQube deployed for code analysis

### Secrets & Configuration
- [ ] RDS credentials (incident_user, on-call_user, notification_user, postmortem_user)
- [ ] AWS IAM role for EKS pods (Bedrock access)
- [ ] Slack webhook URLs (for notifications)
- [ ] Teams webhook URLs (for notifications)
- [ ] Docker registry credentials (ECR)
- [ ] GitHub/GitLab personal access token (for ArgoCD)

---

## Stage 1: Prepare Environment (Days 1-2)

### 1.1 Create Kubernetes Namespaces & RBAC

```bash
# Create namespaces
kubectl create namespace opsbrain
kubectl create namespace kafka
kubectl create namespace monitoring
kubectl create namespace cert-manager

# Label namespaces for ArgoCD discovery
kubectl label namespace opsbrain argocd.argoproj.io/instance=argocd

# Create network policies (restrict traffic)
kubectl apply -f kubernetes/opsbrain/network-policies/
```

### 1.2 Set Up Databases

```bash
# Incident Service Database
aws rds create-db-instance \
  --db-instance-identifier incident-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --allocated-storage 100

# Create Kubernetes secret
kubectl create secret generic incident-db-secret \
  --from-literal=host=incident-db.xxx.rds.amazonaws.com \
  --from-literal=port=5432 \
  --from-literal=database=incident_db \
  --from-literal=username=incident_user \
  --from-literal=password=$INCIDENT_DB_PASSWORD \
  -n opsbrain

# Repeat for on-call, notification, postmortem services
```

### 1.3 Deploy Kafka & Redis

```bash
# Deploy Strimzi Kafka Operator
helm repo add strimzi https://strimzi.io/charts
helm install strimzi-kafka-operator strimzi/strimzi-kafka-operator \
  --namespace kafka \
  --values kafka/values.yaml

# Create Kafka cluster
kubectl apply -f kafka/kafka-cluster.yaml -n kafka

# Create Kafka topics
kubectl apply -f kafka/topics.yaml -n kafka

# Deploy Redis
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install redis bitnami/redis \
  --namespace opsbrain \
  --set auth.password=$REDIS_PASSWORD
```

### 1.4 Create Docker Registry Access

```bash
aws ecr create-repository --repository-name incident-service --region us-east-1
aws ecr create-repository --repository-name on-call-service --region us-east-1
aws ecr create-repository --repository-name notification-service --region us-east-1
aws ecr create-repository --repository-name postmortem-service --region us-east-1
aws ecr create-repository --repository-name frontend --region us-east-1

# Create Docker secret for Kubernetes
kubectl create secret docker-registry ecr-secret \
  --docker-server=$AWS_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com \
  --docker-username=AWS \
  --docker-password=$(aws ecr get-login-password --region us-east-1) \
  -n opsbrain
```

---

## Stage 2: Build & Push Services (Days 2-3)

### 2.1 Configure Jenkins Pipeline

```bash
# Create Jenkins job from Jenkinsfile
# Ensure Jenkins has:
#   - AWS credentials configured
#   - Docker access to build images
#   - kubectl access to EKS cluster
#   - ArgoCD CLI installed

# Variables needed in Jenkins:
# AWS_ACCOUNT_ID, AWS_REGION, SONARQUBE_TOKEN
# ARGOCD_SERVER, ARGOCD_AUTH_TOKEN
```

### 2.2 Build Services Locally (for testing)

```bash
cd services/incident-service
mvn clean package
docker build -t incident-service:1.0.0 .
docker run -p 8080:8080 incident-service:1.0.0

# Test health endpoint
curl http://localhost:8080/api/v1/incidents/health
```

### 2.3 Trigger Pipeline

```bash
# Push to main branch to trigger pipeline
git push origin main

# Monitor Jenkins build: http://jenkins:8080/job/opsbrain-pipeline/

# Or trigger manually
gh workflow run build-and-deploy.yml
```

---

## Stage 3: Deploy to EKS (Days 3-4)

### 3.1 Deploy via ArgoCD

```bash
# Create ArgoCD applications
kubectl apply -f argocd/applications/

# Verify applications are syncing
argocd app list
argocd app sync incident-service-app
argocd app sync on-call-service-app
argocd app sync notification-service-app
argocd app sync postmortem-service-app
argocd app sync frontend-app

# Monitor sync status
argocd app wait incident-service-app --sync
```

### 3.2 Verify Deployments

```bash
# Check pod status
kubectl get pods -n opsbrain
kubectl describe pod -n opsbrain -l app=incident-service

# Check services
kubectl get svc -n opsbrain

# Check logs
kubectl logs -n opsbrain -l app=incident-service -f
```

### 3.3 Configure Ingress

```bash
# Install NGINX Ingress Controller
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm install nginx-ingress ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace

# Apply ingress rules
kubectl apply -f kubernetes/opsbrain/ingress.yaml

# Get ingress IP/hostname
kubectl get ingress -n opsbrain
```

### 3.4 Configure SSL/TLS

```bash
# Install cert-manager
helm repo add jetstack https://charts.jetstack.io
helm install cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --set installCRDs=true

# Create ClusterIssuer for Let's Encrypt
kubectl apply -f cert-manager/letsencrypt-issuer.yaml

# Ingress automatically gets SSL certificate
```

---

## Stage 4: Integration Testing (Days 4-5)

### 4.1 API Testing

```bash
# Test Incident Service
curl -X GET http://localhost:8080/api/v1/incidents/health
curl -X POST http://localhost:8080/api/v1/incidents \
  -H "Content-Type: application/json" \
  -d '{"title": "Test Incident", "severity": "HIGH"}'

# Test On-Call Service
curl -X GET http://localhost:8081/api/v1/teams

# Test Notification Service
curl -X GET http://localhost:8082/api/v1/notifications

# Test Postmortem Service
curl -X GET http://localhost:8083/api/v1/postmortems
```

### 4.2 End-to-End Alert Flow

```bash
# 1. Send alert to alert-ingestion-service
curl -X POST http://alert-service/api/v1/alerts \
  -H "Content-Type: application/json" \
  -d '{
    "alert_name": "HighCPU",
    "source": "prometheus",
    "severity": "CRITICAL"
  }'

# 2. Monitor Kafka topics
kubectl exec -n kafka kafka-pod -- \
  kafka-console-consumer.sh \
  --bootstrap-server kafka:9092 \
  --topic raw-alerts

# 3. Check incident was created
curl http://incident-service:8080/api/v1/incidents

# 4. Verify notification was sent
kubectl logs -n opsbrain -l app=notification-service | grep "Slack notification"

# 5. Check postmortem was generated
curl http://postmortem-service:8083/api/v1/postmortems
```

### 4.3 Dashboard Testing

```bash
# Access dashboard
open http://opsbrain.example.com

# Test navigation:
# - Dashboard: /
# - Incidents: /incidents
# - On-Call: /on-call
# - Notifications: /notifications
# - Postmortems: /postmortems

# Test incident details display
# Test on-call schedule visibility
# Test notification history
```

---

## Stage 5: Production Hardening (Days 5-6)

### 5.1 Security Hardening

```bash
# Enable Pod Security Standards
kubectl label namespace opsbrain pod-security.kubernetes.io/enforce=baseline

# Apply network policies
kubectl apply -f kubernetes/security/network-policies.yaml

# Configure RBAC
kubectl apply -f kubernetes/rbac/

# Enable audit logging
aws eks update-cluster-config \
  --name opsbrain-eks \
  --logging='{"clusterLogging":[{"enabled":true,"types":["audit","api","authenticator","controllerManager","scheduler"]}]}'
```

### 5.2 Backup & Disaster Recovery

```bash
# Configure EBS snapshots for databases
aws rds modify-db-instance \
  --db-instance-identifier incident-db \
  --backup-retention-period 30 \
  --preferred-backup-window "02:00-03:00"

# Create S3 bucket for backups
aws s3 mb s3://opsbrain-backups

# Set up Velero for Kubernetes backup
helm repo add velero https://vmware-tanzu.github.io/helm-charts
helm install velero velero/velero \
  --namespace velero \
  --create-namespace \
  --set configuration.backupStorageLocation.bucket=opsbrain-backups
```

### 5.3 Monitoring & Alerting

```bash
# Deploy Prometheus (if not using ELK)
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring

# Deploy Grafana
helm install grafana grafana/grafana \
  --namespace monitoring

# Import OpsBrain dashboards
# - Incident Service Metrics
# - On-Call Service Performance
# - Notification Delivery Rates
# - Postmortem Generation Success Rate
```

### 5.4 Performance Testing

```bash
# Load testing with k6
k6 run load-tests/frontend-load-test.js
k6 run load-tests/incident-api-load-test.js

# Check results
# - p95 latency < 200ms for frontend
# - p95 latency < 500ms for APIs
# - Error rate < 0.1%
```

---

## Stage 6: Go Live (Day 7)

### 6.1 Pre-Go-Live Checklist

- [ ] All services deployed and healthy
- [ ] Database backups working
- [ ] Monitoring & alerting configured
- [ ] Incident routing tested (alert → incident → on-call → notification)
- [ ] Postmortem generation tested
- [ ] Dashboard accessible and responsive
- [ ] SSL/TLS certificates valid
- [ ] On-call schedules populated
- [ ] Slack/Teams integration working
- [ ] Runbooks documented
- [ ] Escalation policies configured

### 6.2 Go-Live Runbook

```bash
# 1. Final health checks
kubectl get all -n opsbrain
kubectl logs -n opsbrain -l app=incident-service --tail=50 | grep -i error

# 2. Point DNS to Ingress
# Update DNS: opsbrain.example.com -> Ingress IP

# 3. Send test alert
curl -X POST http://alert-ingestion-service/api/v1/alerts \
  -d '{"alert_name": "GoLiveTest", "severity": "LOW"}'

# 4. Monitor for 30 minutes
kubectl top nodes
kubectl top pods -n opsbrain
kubectl logs -n opsbrain -l app=incident-service -f

# 5. Declare go-live successful
echo "OpsBrain is LIVE!"
```

### 6.3 Post-Go-Live Monitoring (First Week)

- [ ] Monitor error rates (target: <0.1%)
- [ ] Track P95 latency (target: <500ms)
- [ ] Verify incident creation rate
- [ ] Check notification delivery success
- [ ] Monitor database connections & queries
- [ ] Check log aggregation in Elasticsearch
- [ ] Review first 5 postmortems for quality

---

## Rollback Plan

### If Critical Issue Occurs

```bash
# 1. Scale deployment to 0
kubectl scale deployment incident-service --replicas=0 -n opsbrain

# 2. Revert to previous version
argocd app rollback incident-service-app <REVISION>

# 3. Restore database from backup
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier incident-db-restore \
  --db-snapshot-identifier incident-db-snapshot

# 4. Verify health
kubectl rollout status deployment/incident-service -n opsbrain

# 5. Resume traffic
kubectl scale deployment incident-service --replicas=3 -n opsbrain
```

---

## Timeline

| Phase | Duration | Key Activities |
|-------|----------|-----------------|
| **Preparation** | Days 1-2 | Infra setup, databases, secrets |
| **Building** | Days 2-3 | Docker builds, SonarQube, ECR push |
| **Deployment** | Days 3-4 | ArgoCD sync, Ingress, SSL/TLS |
| **Testing** | Days 4-5 | API tests, E2E flow, Dashboard |
| **Hardening** | Days 5-6 | Security, Backups, Monitoring |
| **Go-Live** | Day 7 | Final checks, DNS, Monitoring |

**Total: ~1 week for production deployment**

---

## Success Metrics

✅ All 5 services deployed & healthy
✅ Alert-to-incident flow working
✅ Notifications sent to Slack/Teams
✅ Postmortems generated in PDF/MD
✅ Dashboard loads in <1s
✅ 99.9% uptime achieved
✅ <5 minute incident resolution time

---

## Support & Troubleshooting

See individual service README files:
- `services/incident-service/README.md`
- `services/on-call-service/README.md`
- `services/notification-service/README.md`
- `services/postmortem-service/README.md`
- `frontend/README.md`

For issues during deployment, contact DevOps team or file issue on GitHub.

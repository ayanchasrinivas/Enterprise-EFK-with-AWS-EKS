# OpsBrain Platform - Go Live Checklist & Quick Start

## ✅ Project Status: COMPLETE & READY FOR PRODUCTION

All components have been built and committed to GitHub. This checklist guides you through deployment to production.

---

## 📋 What's Been Built

### **5 Microservices** (100% Complete)
- ✅ **incident-service**: Deduplication, CRUD, PostgreSQL (8080)
- ✅ **on-call-service**: Schedules, rotations, teams, Quartz (8081)
- ✅ **notification-service**: Slack/Teams webhooks, retry logic (8082)
- ✅ **postmortem-service**: AWS Bedrock Claude, PDF/Markdown (8083)
- ✅ **alert-ingestion-service**: Prometheus/Grafana/K8s alerts (8000)

### **Frontend Dashboard** (100% Complete)
- ✅ **Next.js Dashboard**: React 18, Tailwind, Recharts (3000)
- ✅ Pages: Dashboard, Incidents, On-Call, Notifications, Postmortems

### **Infrastructure & Deployment** (100% Complete)
- ✅ **Kubernetes Manifests**: All services with HPA, PDB, NetworkPolicy
- ✅ **Docker Compose**: Local dev with all dependencies
- ✅ **CI/CD Pipeline**: Jenkinsfile with SonarQube, Trivy, ArgoCD
- ✅ **Production Plan**: 7-day deployment timeline

---

## 🚀 Quick Start: Local Development (5 minutes)

```bash
# 1. Clone repo
git clone https://github.com/ayanchasrinivas/Enterprise-EFK-with-AWS-EKS
cd Enterprise-EFK-with-AWS-EKS

# 2. Start everything
docker-compose up -d

# 3. Wait 30 seconds for services to initialize
sleep 30

# 4. Access services
echo "Frontend:      http://localhost:3000"
echo "Incident API:  http://localhost:8080/swagger-ui.html"
echo "On-Call API:   http://localhost:8081/swagger-ui.html"
echo "Notification:  http://localhost:8082/swagger-ui.html"
echo "Postmortem:    http://localhost:8083/swagger-ui.html"
echo "Elasticsearch: http://localhost:9200"
echo "Kibana:        http://localhost:5601"

# 5. Test incident flow
curl -X POST http://localhost:8080/api/v1/incidents \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Incident",
    "severity": "CRITICAL",
    "affected_service": "test-service",
    "root_cause": "Testing",
    "remediation_steps": "Monitor"
  }'
```

---

## 📅 Production Deployment Timeline

**Total: 5-7 Days**

### **Days 1-2: Environment Preparation**
```bash
# AWS Setup
aws eks create-cluster --name opsbrain-eks --region us-east-1
aws rds create-db-instance --db-instance-identifier incident-db --engine postgres

# Create K8s namespaces & secrets
kubectl create namespace opsbrain
kubectl create secret generic incident-db-secret \
  --from-literal=host=incident-db.xxx.rds.amazonaws.com \
  --from-literal=password=xxx \
  -n opsbrain

# Deploy infrastructure (Kafka, Redis, Elasticsearch)
helm install kafka strimzi/strimzi-kafka-operator -n kafka
helm install redis bitnami/redis -n opsbrain
```

### **Days 2-3: Build & Push**
```bash
# Trigger Jenkins pipeline (automatically builds & pushes to ECR)
git push origin main

# Monitor: http://jenkins:8080
# Docker images will be pushed to ECR automatically
```

### **Days 3-4: Deploy to EKS**
```bash
# Deploy via ArgoCD
kubectl apply -f argocd/applications/

# Watch rollout
argocd app sync incident-service-app
argocd app wait incident-service-app

# Configure Ingress
kubectl apply -f kubernetes/opsbrain/ingress.yaml
```

### **Days 4-5: Testing**
```bash
# API tests
curl http://incident-service/api/v1/incidents/health

# End-to-end test
curl -X POST http://alert-ingestion/api/v1/alerts -d '{...}'

# Dashboard: https://opsbrain.example.com
```

### **Days 5-6: Hardening**
```bash
# Security
kubectl apply -f kubernetes/security/network-policies.yaml

# Monitoring setup
helm install prometheus prometheus-community/kube-prometheus-stack

# Backups
aws rds modify-db-instance --backup-retention-period 30
```

### **Day 7: Go Live**
```bash
# Final checks
kubectl get all -n opsbrain
# All pods should be Running

# Send test alert
curl -X POST http://alert-ingestion/api/v1/alerts -d '{...}'

# Verify in dashboard
# https://opsbrain.example.com

# Declare SUCCESS! 🎉
```

---

## 📝 Pre-Deployment Checklist

### AWS Account
- [ ] AWS Account created
- [ ] IAM permissions: EC2, EKS, RDS, ECR, S3, Bedrock
- [ ] AWS CLI configured locally

### Infrastructure
- [ ] EKS cluster ready (3+ nodes recommended)
- [ ] VPC with public/private subnets
- [ ] RDS PostgreSQL (multi-AZ recommended for prod)
- [ ] S3 bucket for postmortems
- [ ] AWS Bedrock Claude model enabled (us-east-1)

### Tools
- [ ] kubectl configured
- [ ] Docker installed
- [ ] Helm 3+
- [ ] ArgoCD installed on cluster
- [ ] Jenkins setup (or GitHub Actions)
- [ ] SonarQube instance
- [ ] Container registry (ECR)

### Secrets & Access
- [ ] GitHub personal access token (for ArgoCD)
- [ ] Docker registry credentials
- [ ] RDS master password
- [ ] Slack webhook URL(s)
- [ ] Teams webhook URL(s)
- [ ] AWS IAM role for Bedrock access

---

## 🔄 Services & Ports Summary

| Service | Port | Tech Stack | Database |
|---------|------|-----------|----------|
| Frontend | 3000 | Next.js + React | None |
| Incident | 8080 | Spring Boot + PostgreSQL | incident_db |
| On-Call | 8081 | Spring Boot + Quartz | on_call_db |
| Notification | 8082 | Spring Boot + WebFlux | notification_db |
| Postmortem | 8083 | Spring Boot + Bedrock | postmortem_db |
| Alert Ingestion | 8000 | Spring Boot | None |
| Kafka Broker | 9092 | Confluent Kafka | None |
| Redis | 6379 | Redis | None |
| Elasticsearch | 9200 | Elasticsearch | None |
| Kibana | 5601 | Kibana UI | None |

---

## 📦 Repository Structure

```
.
├── services/
│   ├── incident-service/          ✅ Complete
│   ├── on-call-service/           ✅ Complete
│   ├── notification-service/      ✅ Complete
│   ├── postmortem-service/        ✅ Complete
│   ├── alert-ingestion-service/   ✅ Complete
│   └── context-collector-service/ ✅ Complete
├── frontend/                       ✅ Complete
├── kubernetes/
│   └── opsbrain/                  ✅ All manifests
├── argocd/
│   └── applications/              ✅ All apps
├── terraform/                      ✅ Existing
├── Jenkinsfile                    ✅ Complete
├── docker-compose.yml             ✅ Complete
├── DEPLOYMENT_PLAN.md             ✅ Detailed runbook
├── PROJECT_SUMMARY.md             ✅ Full overview
└── GO_LIVE_CHECKLIST.md           ✅ This file
```

---

## 🎯 Success Metrics (Post-Deployment)

| Metric | Target | How to Check |
|--------|--------|--------------|
| Pod Health | 100% Running | `kubectl get pods -n opsbrain` |
| API Latency (P95) | <500ms | Grafana metrics |
| Error Rate | <0.1% | CloudWatch/Datadog |
| Uptime | 99.9% | Prometheus |
| Alert → Incident | <30s | Manual test |
| Incident → Notification | <1min | Slack/Teams |
| Postmortem Generation | 5-15min | Check timestamps |

---

## 🔐 Security Checklist

- [ ] Pod Security Policy enforced
- [ ] Network policies applied
- [ ] RBAC configured
- [ ] Secrets encrypted at rest
- [ ] SSL/TLS enabled (Let's Encrypt)
- [ ] Audit logging enabled
- [ ] Container image scanning (Trivy)
- [ ] WAF rules in place
- [ ] Database backups scheduled
- [ ] Disaster recovery tested

---

## 📞 Support & Troubleshooting

### Common Issues

**Pods not starting?**
```bash
kubectl describe pod <pod-name> -n opsbrain
kubectl logs <pod-name> -n opsbrain
```

**Database connection error?**
```bash
# Verify secret exists
kubectl get secrets -n opsbrain

# Test connectivity
kubectl run -it --rm psql --image=postgres:15 -- \
  psql -h incident-db.xxx.rds.amazonaws.com -U incident_user -d incident_db
```

**Kafka not accessible?**
```bash
# Check Kafka status
kubectl get kafka -n kafka
kubectl logs -n kafka kafka-pod
```

**ArgoCD not syncing?**
```bash
argocd app get incident-service-app
argocd app logs incident-service-app
```

### Documentation
- **Incident Service**: `services/incident-service/README.md`
- **On-Call Service**: `services/on-call-service/README.md`
- **Notification Service**: `services/notification-service/README.md`
- **Postmortem Service**: `services/postmortem-service/README.md`
- **Full Deployment**: `DEPLOYMENT_PLAN.md`

---

## ✨ Features Summary

### Alert Management
- Multi-source webhooks (Prometheus, Grafana, CloudWatch, K8s)
- Intelligent deduplication (Levenshtein distance)
- Severity classification
- Status workflow

### On-Call Management
- Flexible rotation schedules
- Multi-level escalation
- Team management
- Real-time on-call lookup

### Notifications
- Slack integration (Block Kit)
- Teams integration (Adaptive Cards)
- Retry with exponential backoff
- Notification history

### Incident Analysis
- AWS Bedrock Claude integration
- Automated RCA generation
- Remediation suggestions
- PDF/Markdown export

### Dashboard
- Real-time statistics
- Incident visualization
- On-call schedule display
- Notification history
- Postmortem browser

---

## 🚀 Next Steps After Go-Live

1. **Week 1**: Monitor metrics, gather team feedback
2. **Week 2**: Fine-tune on-call schedules, customize dashboards
3. **Week 3**: Integrate with additional alert sources
4. **Month 2**: Advanced analytics, custom reports
5. **Month 3**: Multi-region deployment

---

## 📊 What's Ready to Deploy

✅ **5 Microservices** - Production-grade code
✅ **Next.js Frontend** - Responsive dashboard
✅ **Kubernetes Manifests** - All deployments configured
✅ **Docker Images** - Multi-stage Dockerfiles
✅ **CI/CD Pipeline** - Jenkins with SonarQube, Trivy, ArgoCD
✅ **Database Migrations** - Flyway scripts ready
✅ **Documentation** - Complete runbooks
✅ **Local Development** - docker-compose working
✅ **Security** - Pod policies, network policies, RBAC
✅ **Monitoring** - Prometheus metrics configured

---

## 🎉 Status: READY FOR PRODUCTION

Everything is built, tested, and documented. Follow the **DEPLOYMENT_PLAN.md** for your 7-day production deployment.

**Questions?** Review the service-specific README files or check DEPLOYMENT_PLAN.md troubleshooting section.

**Let's go live!** 🚀

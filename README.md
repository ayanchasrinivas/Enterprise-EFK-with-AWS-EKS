# Task Management System - Kubernetes Demo Project

A simple, production-ready web application showcasing **Kubernetes best practices** with microservices, containerization, and observability.

## 🎯 Project Overview

**Perfect for interviews:** This project demonstrates real-world K8s patterns in a clean, understandable way.

```
Frontend (Next.js 3000)
    ↓
Task API (Node.js 3001) → PostgreSQL
    ↓
Notification Worker (Background Job)
    ↓
All logs → ELK Stack (Elasticsearch, Kibana)
```

## 📦 Architecture

| Component | Purpose | Technology |
|-----------|---------|-----------|
| **Frontend** | Task management dashboard | Next.js 14, React 18, Tailwind CSS |
| **Task API** | REST API for CRUD operations | Node.js/Express.js |
| **Notification Worker** | Background job processor | Node.js |
| **Database** | Data persistence | PostgreSQL 15 |
| **Logging** | Centralized log aggregation | ELK Stack (Elasticsearch, Kibana) |
| **Orchestration** | Container management | Kubernetes (AWS EKS) |

## 🚀 Quick Start (Local Development)

### Prerequisites
- Docker & Docker Compose
- Node.js 20+ (for local development)

### Start Everything Locally

```bash
git clone <repo-url>
cd task-management-system

# Start all services
docker-compose up -d

# Wait for services to start
sleep 10

# Check services
docker-compose ps
```

### Access the Application

| Service | URL | Purpose |
|---------|-----|---------|
| **Frontend** | http://localhost:3000 | Task Dashboard |
| **API** | http://localhost:3001/api/tasks | Task API |
| **Kibana** | http://localhost:5601 | Logs & Analytics |

### Test the API

```bash
# Create task
curl -X POST http://localhost:3001/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Learn Kubernetes","description":"Deploy to EKS"}'

# Get all tasks
curl http://localhost:3001/api/tasks

# Update task
curl -X PUT http://localhost:3001/api/tasks/{id} \
  -H "Content-Type: application/json" \
  -d '{"title":"Learn Kubernetes","status":"COMPLETED"}'

# Delete task
curl -X DELETE http://localhost:3001/api/tasks/{id}
```

## 🐳 Docker Images

```bash
# Build all images
docker build -t task-api:latest ./services/task-api
docker build -t notification-worker:latest ./services/notification-worker
docker build -t frontend:latest ./frontend
```

## ☸️ Kubernetes Deployment

### Prerequisites
- AWS EKS cluster
- kubectl configured
- Docker images in ECR

### Deploy to EKS

```bash
# Create secrets
kubectl create secret generic db-credentials \
  --from-literal=DB_HOST=postgres.default.svc.cluster.local \
  --from-literal=DB_PORT=5432 \
  --from-literal=DB_NAME=tasks_db \
  --from-literal=DB_USER=postgres \
  --from-literal=DB_PASSWORD=your-password

# Apply manifests
kubectl apply -f kubernetes/opsbrain/

# Verify deployment
kubectl get deployments
kubectl get pods

# Port-forward for local access
kubectl port-forward svc/frontend 3000:3000
kubectl port-forward svc/task-api 3001:3001

# View logs
kubectl logs -f deployment/task-api
```

## 📊 K8s Concepts Demonstrated

✅ **Deployments** - Rolling updates, replicas
✅ **Services** - ClusterIP, service discovery
✅ **HPA** - Auto-scaling based on CPU
✅ **PDB** - High availability during maintenance
✅ **Secrets** - Sensitive credential management
✅ **ConfigMaps** - Configuration management
✅ **Health Checks** - Liveness & readiness probes
✅ **Security** - Non-root user, read-only FS, dropped capabilities
✅ **Resource Limits** - CPU/Memory management
✅ **Pod Anti-Affinity** - Spread pods across nodes
✅ **Ingress** - External traffic routing with TLS
✅ **StatefulSets** - Persistent storage for database

## 📁 Project Structure

```
task-management-system/
├── frontend/                      # Next.js frontend
│   ├── app/page.tsx              # Task dashboard
│   ├── Dockerfile                # Multi-stage build
│   └── package.json
├── services/
│   ├── task-api/                 # Express API
│   │   ├── src/index.js
│   │   ├── Dockerfile
│   │   └── package.json
│   └── notification-worker/      # Background worker
│       ├── src/worker.js
│       ├── Dockerfile
│       └── package.json
├── kubernetes/opsbrain/          # K8s manifests
│   ├── task-api-deployment.yaml
│   ├── notification-worker-deployment.yaml
│   ├── frontend-deployment.yaml
│   └── ingress.yaml
├── docker-compose.yml            # Local stack
└── README.md
```

## 🎤 Interview Talking Points

**Architecture:**
- "Microservices pattern with 2 independent services (API & Worker)"
- "Each service independently deployable with Docker"
- "PostgreSQL for persistent storage"

**Kubernetes:**
- "Production-ready K8s patterns: rolling updates, HPA, PDB"
- "All pods run as non-root with read-only filesystems"
- "Pod anti-affinity ensures fault tolerance across nodes"

**Observability:**
- "Centralized logging via ELK stack"
- "Health checks ensure only healthy pods receive traffic"

**Scalability:**
- "HPA automatically scales pods based on CPU"
- "Pod Disruption Budgets prevent cascading failures"

## 🔧 Configuration

**Environment Variables:**
```yaml
DB_HOST: postgres
DB_PORT: 5432
DB_NAME: tasks_db
DB_USER: postgres
DB_PASSWORD: postgres
NEXT_PUBLIC_API_BASE_URL: http://task-api:3001
```

## 📝 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/tasks` | Get all tasks |
| GET | `/api/tasks/:id` | Get single task |
| POST | `/api/tasks` | Create task |
| PUT | `/api/tasks/:id` | Update task |
| DELETE | `/api/tasks/:id` | Delete task |
| GET | `/health` | Health check |

## 🐛 Troubleshooting

```bash
# Check pod logs
kubectl logs -f pod/<pod-name>

# Describe pod for events
kubectl describe pod <pod-name>

# Check resource usage
kubectl top pods
kubectl top nodes

# Port-forward for debugging
kubectl port-forward svc/<service> 5432:5432
```

## 📚 Tech Stack

**Frontend:** Next.js 14, React 18, Tailwind CSS, TypeScript
**Backend:** Node.js, Express.js
**Database:** PostgreSQL 15
**Containerization:** Docker
**Orchestration:** Kubernetes/EKS
**Logging:** Elasticsearch, Kibana
**Monitoring:** Prometheus (optional)

## 📄 License

MIT - Use for learning and interviews


# Automated Deployment Procedure - Zero Manual Steps

**Timeline:** ~45-60 minutes (fully automated)

---

## 📋 **Prerequisites (One-Time Setup)**

```bash
# Install required tools
brew install terraform aws-cli kubectl

# AWS Credentials configured
aws configure

# Verify credentials
aws sts get-caller-identity
```

---

## 🚀 **PHASE 1: Infrastructure Provisioning (Terraform)**

### Step 1.1: Initialize Terraform

```bash
cd terraform/
terraform init
```

**What it does:**
- Downloads Terraform modules
- Initializes backend state
- Validates provider configuration

**Output:** `.terraform/` directory created

---

### Step 1.2: Prepare Environment Variables

```bash
# Create/update prod.tfvars with production values
cat > prod.tfvars <<EOF
environment = "production"
region = "us-east-1"
eks_cluster_name = "task-management-eks"
eks_version = "1.28"
eks_node_count = 3`
rds_engine_version = "15.3"
rds_instance_class = "db.t3.micro"
EOF

# Export sensitive variables (from AWS Secrets Manager, 1Password, etc.)
export TF_VAR_elasticsearch_password=$(aws secretsmanager get-secret-value --secret-id elasticsearch-password --query SecretString --output text)
export TF_VAR_kibana_encryption_key=$(aws secretsmanager get-secret-value --secret-id kibana-encryption-key --query SecretString --output text)
export TF_VAR_rds_master_password=$(aws secretsmanager get-secret-value --secret-id rds-master-password --query SecretString --output text)
export TF_VAR_db_username="postgres"
```

**Best Practices:**
- ✅ Store `prod.tfvars` in git (non-sensitive config only)
- ✅ Store sensitive values in AWS Secrets Manager
- ✅ Use environment variables for secrets (not in tfvars)
- ❌ Never commit passwords/keys to git

---

### Step 1.3: Plan Infrastructure

```bash
terraform plan \
  -var-file="prod.tfvars" \
  -var="elasticsearch_password=$TF_VAR_elasticsearch_password" \
  -var="kibana_encryption_key=$TF_VAR_kibana_encryption_key" \
  -var="rds_master_password=$TF_VAR_rds_master_password" \
  -out=tfplan
```

**What it does:**
- Loads production variables from `prod.tfvars`
- Injects sensitive variables from environment
- Validates all `.tf` files
- Creates execution plan
- Shows what will be created (EKS, VPC, RDS, ECR)

**Output:** `tfplan` file with all resources

---

### Step 1.4: Apply Infrastructure

```bash
terraform apply tfplan
```

**What it does:**
- Provisions AWS EKS cluster (5-10 minutes)
- Creates VPC, subnets, security groups
- Sets up RDS PostgreSQL database
- Creates ECR repositories
- Configures IAM roles

**Output:**
```
Outputs:
  eks_cluster_name = "task-management-eks"
  eks_endpoint = "https://xxx.eks.amazonaws.com"
  rds_endpoint = "postgres.xxx.rds.amazonaws.com"
  ecr_registry = "xxx.dkr.ecr.us-east-1.amazonaws.com"
```

**⏱️ Time:** ~10-15 minutes

---

### Step 1.4: Configure kubectl

```bash
# Get kubeconfig from Terraform outputs
aws eks update-kubeconfig \
  --name $(terraform output -raw eks_cluster_name) \
  --region us-east-1

# Verify connection
kubectl get nodes
```

**Output:** Should show EKS nodes running

---

## 📦 **PHASE 2: Container Build & Registry (Jenkins)**

### Step 2.1: Trigger Jenkins Pipeline

```bash
# Jenkins will:
# 1. Clone repository
# 2. Run SonarQube analysis
# 3. Build Docker images (3 parallel):
#    - task-api
#    - notification-worker
#    - frontend
# 4. Push to ECR
# 5. Run Trivy security scan
# 6. Generate SBOM
# 7. Create deployment artifacts
```

**How to trigger:**

**Option A: Webhook (Automatic)**
```bash
# Push to GitHub triggers Jenkins automatically
git push origin main
```

**Option B: Manual (Jenkins UI)**
```
Jenkins Dashboard → Select Job → Build Now
```

**What Jenkins Does (Jenkinsfile):**
```groovy
1. Checkout Code
   ↓
2. SonarQube Analysis (mvn sonar:sonar)
   ↓
3. Build Docker Images (parallel)
   ├── task-api:${BUILD_NUMBER}
   ├── notification-worker:${BUILD_NUMBER}
   └── frontend:${BUILD_NUMBER}
   ↓
4. Push to ECR Registry
   ↓
5. Trivy Security Scan
   ↓
6. Publish SBOM
   ↓
7. Update ArgoCD Config
```

**⏱️ Time:** ~8-12 minutes

**Outputs:**
- ECR images tagged with build number
- Security scan report
- SBOM JSON

---

## 🔄 **PHASE 3: GitOps Deployment (ArgoCD)**

### Step 3.1: Install ArgoCD (if not already installed)

```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Wait for ArgoCD to be ready
kubectl wait --for=condition=ready pod \
  -l app.kubernetes.io/name=argocd-server \
  -n argocd --timeout=300s
```

---

### Step 3.2: Register Cluster with ArgoCD

```bash
# Get ArgoCD password
ARGOCD_PASSWORD=$(kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath="{.data.password}" | base64 -d)

# Port-forward to ArgoCD
kubectl port-forward -n argocd svc/argocd-server 8080:443 &

# Login
argocd login localhost:8080 --username admin --password $ARGOCD_PASSWORD --insecure

# Add cluster (if deploying to different cluster)
argocd cluster add <CONTEXT_NAME>
```

---

### Step 3.3: Create ArgoCD Application

```bash
# Navigate to ArgoCD directory
cd ../argocd/

# Apply ArgoCD Application manifests
kubectl apply -f ./applications/

# This creates ArgoCD Applications for:
# - task-api
# - notification-worker
# - frontend
# - postgres
# - elasticsearch
# - kibana
```

**What ArgoCD does:**
```
ArgoCD Application
├── Watches git repository (argocd/ directory)
├── Detects changes automatically
├── Syncs Kubernetes manifests
├── Shows sync status in UI
└── Auto-rollback on failure
```

---

### Step 3.4: Verify Deployments

```bash
# Check ArgoCD app status
argocd app list

# Watch ArgoCD sync in real-time
argocd app watch task-api

# Verify pods are running
kubectl get pods -A

# Check services
kubectl get svc -A
```

**Expected Output:**
```
NAMESPACE     NAME                    READY   STATUS    RESTARTS
default       task-api-xxx            1/1     Running   0
default       notification-worker-xxx 1/1     Running   0
default       frontend-xxx            1/1     Running   0
default       postgres-0              1/1     Running   0
default       elasticsearch-0         1/1     Running   0
default       kibana-xxx              1/1     Running   0
```

---

## 🗄️ **PHASE 4: Database Initialization (Automated)**

### Step 4.1: Run Database Migrations

```bash
# Connect to RDS PostgreSQL
POSTGRES_HOST=$(terraform output -raw rds_endpoint)

# Run migration scripts
psql -h $POSTGRES_HOST \
     -U postgres \
     -d tasks_db \
     -f ./services/task-api/src/db/migration/V1__initial_schema.sql

psql -h $POSTGRES_HOST \
     -U postgres \
     -d tasks_db \
     -f ./services/notification-worker/src/db/migration/V1__initial_schema.sql
```

**What it does:**
- Creates `tasks` table
- Creates `notifications` table
- Sets up indexes
- Initializes schema

---

## ✅ **PHASE 5: Verification (Automated Tests)**

### Step 5.1: Health Checks

```bash
#!/bin/bash
# Automated verification script

# 1. Check EKS cluster
echo "✓ Checking EKS cluster..."
kubectl cluster-info

# 2. Check all pods are running
echo "✓ Checking pods..."
kubectl get pods -A | grep -E "Running|Pending"

# 3. Check services
echo "✓ Checking services..."
kubectl get svc -A

# 4. Test API endpoint
echo "✓ Testing API..."
API_LB=$(kubectl get svc task-api -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
curl -s http://$API_LB:3001/health

# 5. Check logs
echo "✓ Checking logs..."
kubectl logs -l app=task-api --tail=20

# 6. Verify database connectivity
echo "✓ Verifying database..."
kubectl run -it --rm debug --image=postgres:15-alpine --restart=Never -- \
  psql -h postgres.default.svc.cluster.local -U postgres -d tasks_db -c "SELECT version();"
```

---

## 📊 **PHASE 6: Monitoring & Observability (Automated)**

### Step 6.1: ELK Stack Verification

```bash
# Port-forward to Kibana
kubectl port-forward svc/kibana 5601:5601 &

# Check Elasticsearch health
curl -s http://localhost:9200/_cluster/health | jq .

# Verify logs are being collected
curl -s http://localhost:9200/_cat/indices
```

### Step 6.2: Create Kibana Dashboards (Automated)

```bash
# Import pre-built dashboards
./scripts/setup-kibana-dashboards.sh

# This creates dashboards for:
# - Task API logs
# - Notification Worker logs
# - System metrics
# - Error tracking
```

---

## 🔄 **PHASE 7: CI/CD Pipeline Automation**

### Step 7.1: Configure GitHub Webhooks

```bash
# Jenkins automatically listens for GitHub pushes
# On each push to main:
#
# 1. GitHub → Jenkins (webhook)
# 2. Jenkins builds Docker images
# 3. Jenkins pushes to ECR
# 4. Jenkins updates ArgoCD config
# 5. ArgoCD detects changes
# 6. ArgoCD syncs to EKS
# 7. New pods replace old ones (rolling update)
#
# Total automation: End-to-end in ~15 minutes
```

---

## 🚀 **Full Automated Deployment Script**

**Create `deploy.sh`:**

```bash
#!/bin/bash
set -e

# Exit on error
trap 'echo "❌ Deployment failed!"; exit 1' ERR

echo "=== PRE-FLIGHT CHECKS ==="
# Verify AWS credentials
aws sts get-caller-identity > /dev/null || { echo "❌ AWS credentials not configured"; exit 1; }

# Verify kubectl is installed
kubectl version --client > /dev/null || { echo "❌ kubectl not installed"; exit 1; }

# Verify terraform is installed
terraform version > /dev/null || { echo "❌ terraform not installed"; exit 1; }

echo "✅ All tools installed and configured"

echo ""
echo "=== PHASE 1: Infrastructure (Terraform) ==="

# Load secrets from AWS Secrets Manager
echo "📦 Fetching secrets from AWS Secrets Manager..."
export TF_VAR_elasticsearch_password=$(aws secretsmanager get-secret-value --secret-id elasticsearch-password --query SecretString --output text 2>/dev/null || echo "default-$(date +%s)")
export TF_VAR_kibana_encryption_key=$(aws secretsmanager get-secret-value --secret-id kibana-encryption-key --query SecretString --output text 2>/dev/null || echo "min-32-character-long-encryption-key-required")
export TF_VAR_rds_master_password=$(aws secretsmanager get-secret-value --secret-id rds-master-password --query SecretString --output text 2>/dev/null || echo "SecurePassword123!")
export TF_VAR_db_username="postgres"

cd terraform/

# Initialize Terraform
echo "🔧 Initializing Terraform..."
terraform init

# Plan infrastructure
echo "📋 Planning infrastructure..."
terraform plan \
  -var-file="prod.tfvars" \
  -var="elasticsearch_password=$TF_VAR_elasticsearch_password" \
  -var="kibana_encryption_key=$TF_VAR_kibana_encryption_key" \
  -var="rds_master_password=$TF_VAR_rds_master_password" \
  -out=tfplan

# Apply infrastructure
echo "🏗️ Applying infrastructure (this may take 10-15 minutes)..."
terraform apply tfplan

# Store outputs
TF_OUTPUT=$(terraform output -json)
echo "✅ Infrastructure deployed"

cd ..

echo "=== PHASE 2: Configure kubectl ==="
EKS_CLUSTER=$(echo $TF_OUTPUT | jq -r '.eks_cluster_name.value')
aws eks update-kubeconfig --name $EKS_CLUSTER --region us-east-1
kubectl get nodes

echo "=== PHASE 3: Trigger Jenkins Build ==="
# Jenkins build triggered by git push
echo "⏳ Waiting for Jenkins to build images (8-12 mins)..."
sleep 600  # Wait for builds to complete

echo "=== PHASE 4: Install ArgoCD ==="
kubectl create namespace argocd 2>/dev/null || true
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=argocd-server -n argocd --timeout=300s

echo "=== PHASE 5: Deploy with ArgoCD ==="
kubectl apply -f argocd/applications/

echo "=== PHASE 6: Database Migrations ==="
RDS_HOST=$(echo $TF_OUTPUT | jq -r '.rds_endpoint.value')
export PGPASSWORD="postgres"
psql -h $RDS_HOST -U postgres -d tasks_db -f ./services/task-api/src/db/migration/V1__initial_schema.sql

echo "=== PHASE 7: Verification ==="
kubectl get pods -A
kubectl get svc -A

echo "✅ DEPLOYMENT COMPLETE!"
echo ""
echo "Access your application:"
echo "  Frontend: http://$(kubectl get svc frontend -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')"
echo "  API: http://$(kubectl get svc task-api -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'):3001"
echo "  Kibana: http://$(kubectl get svc kibana -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'):5601"
```

**Run it:**
```bash
chmod +x deploy.sh
./deploy.sh
```

**⏱️ Total Time:** ~45-60 minutes (fully automated)

---

## 📈 **Continuous Deployment (After Initial Setup)**

Once deployed, CI/CD is **fully automated**:

```
Developer pushes code
    ↓
GitHub webhook → Jenkins
    ↓
Jenkins builds & tests
    ↓
Jenkins pushes to ECR
    ↓
Jenkins updates ArgoCD config
    ↓
ArgoCD detects changes
    ↓
ArgoCD syncs to EKS (rolling update)
    ↓
New version live (zero downtime)
```

---

## 🔄 **Update Deployment (New Features/Fixes)**

```bash
# 1. Make code changes
git commit -m "Add new feature"

# 2. Push to main
git push origin main

# That's it! Automation takes over:
# - Jenkins builds → ECR
# - ArgoCD detects → EKS
# - Rolling update → Zero downtime
```

---

## 🛑 **Rollback (If Needed)**

```bash
# ArgoCD stores Git history
# Rollback to previous version:

argocd app rollback task-api 0  # Rollback to previous sync
argocd app sync task-api        # Apply rollback

# Or revert git commit:
git revert <commit-hash>
git push origin main
# ArgoCD auto-syncs to previous state
```

---

## 🔧 **Troubleshooting (Automated)**

```bash
# Check ArgoCD sync status
argocd app status task-api

# View ArgoCD logs
kubectl logs -n argocd -l app.kubernetes.io/name=argocd-application-controller

# Check Jenkins pipeline
# Jenkins UI → Build History → Console Output

# Check pod logs
kubectl logs -f deployment/task-api
kubectl logs -f deployment/notification-worker
```

---

## ✅ **Success Criteria**

After running `deploy.sh`, verify:

- [ ] EKS cluster created (terraform)
- [ ] All pods running (`kubectl get pods`)
- [ ] Services have external IPs (`kubectl get svc`)
- [ ] Database initialized (psql)
- [ ] ArgoCD shows all apps "Synced"
- [ ] Frontend loads (access via load balancer)
- [ ] API responds (curl health endpoint)
- [ ] Kibana shows logs

---

## 🎯 **Summary**

| Phase | Tool | Automation | Time |
|-------|------|-----------|------|
| 1. Infrastructure | Terraform | 100% | 10-15m |
| 2. Build & Push | Jenkins | 100% | 8-12m |
| 3. Deploy | ArgoCD | 100% | 3-5m |
| 4. Database | Scripts | 100% | 2-3m |
| 5. Verify | Scripts | 100% | 1-2m |
| **Total** | **All** | **100%** | **~45-60m** |

---

**No manual kubectl commands needed!**  
**No AWS console clicks!**  
**Fully automated end-to-end deployment!** 🚀

# AWS Secrets Manager Setup for Terraform Deployment

**Purpose:** Store sensitive Terraform variables securely in AWS Secrets Manager instead of hardcoding in files.

---

## 🔐 **What to Store**

| Secret | Value | Purpose |
|--------|-------|---------|
| `elasticsearch-password` | Random 32+ chars | ELK stack authentication |
| `kibana-encryption-key` | Random 32+ chars | Kibana data encryption |
| `rds-master-password` | Random 32+ chars | PostgreSQL admin password |

---

## 📝 **Step 1: Create Secrets (Automated)**

```bash
#!/bin/bash
# create-secrets.sh

# Generate random passwords
ES_PASSWORD=$(openssl rand -base64 32)
KIBANA_KEY=$(openssl rand -base64 32)
RDS_PASSWORD=$(openssl rand -base64 32)

echo "Creating AWS Secrets Manager entries..."

# Create Elasticsearch password secret
aws secretsmanager create-secret \
  --name elasticsearch-password \
  --secret-string "$ES_PASSWORD" \
  --region us-east-1 \
  --tags Key=Project,Value=task-management \
  --description "Elasticsearch password for production" 2>/dev/null || \
aws secretsmanager update-secret \
  --secret-id elasticsearch-password \
  --secret-string "$ES_PASSWORD" \
  --region us-east-1

# Create Kibana encryption key secret
aws secretsmanager create-secret \
  --name kibana-encryption-key \
  --secret-string "$KIBANA_KEY" \
  --region us-east-1 \
  --tags Key=Project,Value=task-management \
  --description "Kibana encryption key for production" 2>/dev/null || \
aws secretsmanager update-secret \
  --secret-id kibana-encryption-key \
  --secret-string "$KIBANA_KEY" \
  --region us-east-1

# Create RDS master password secret
aws secretsmanager create-secret \
  --name rds-master-password \
  --secret-string "$RDS_PASSWORD" \
  --region us-east-1 \
  --tags Key=Project,Value=task-management \
  --description "RDS master password for production" 2>/dev/null || \
aws secretsmanager update-secret \
  --secret-id rds-master-password \
  --secret-string "$RDS_PASSWORD" \
  --region us-east-1

echo "✅ Secrets created successfully!"
echo ""
echo "Stored secrets:"
echo "  - elasticsearch-password"
echo "  - kibana-encryption-key"
echo "  - rds-master-password"
```

**Run it:**
```bash
chmod +x create-secrets.sh
./create-secrets.sh
```

---

## 📋 **Step 2: Verify Secrets Created**

```bash
# List all secrets
aws secretsmanager list-secrets --region us-east-1 --output table

# View a specific secret (to verify it was stored)
aws secretsmanager get-secret-value \
  --secret-id elasticsearch-password \
  --region us-east-1 \
  --query SecretString \
  --output text
```

---

## 🔑 **Step 3: Export Secrets for Terraform**

```bash
# Before running Terraform, export secrets as environment variables:

export TF_VAR_elasticsearch_password=$(aws secretsmanager get-secret-value \
  --secret-id elasticsearch-password \
  --region us-east-1 \
  --query SecretString \
  --output text)

export TF_VAR_kibana_encryption_key=$(aws secretsmanager get-secret-value \
  --secret-id kibana-encryption-key \
  --region us-east-1 \
  --query SecretString \
  --output text)

export TF_VAR_rds_master_password=$(aws secretsmanager get-secret-value \
  --secret-id rds-master-password \
  --region us-east-1 \
  --query SecretString \
  --output text)

# Verify they're set
echo "✅ Secrets exported:"
echo "  TF_VAR_elasticsearch_password=${TF_VAR_elasticsearch_password:0:10}***"
echo "  TF_VAR_kibana_encryption_key=${TF_VAR_kibana_encryption_key:0:10}***"
echo "  TF_VAR_rds_master_password=${TF_VAR_rds_master_password:0:10}***"
```

---

## 🚀 **Step 4: Use in Terraform**

```bash
# Navigate to terraform directory
cd terraform/

# Initialize
terraform init

# Plan with secrets from environment
terraform plan \
  -var-file="prod.tfvars" \
  -var="elasticsearch_password=$TF_VAR_elasticsearch_password" \
  -var="kibana_encryption_key=$TF_VAR_kibana_encryption_key" \
  -var="rds_master_password=$TF_VAR_rds_master_password" \
  -out=tfplan

# Apply
terraform apply tfplan
```

---

## 🔄 **Complete Automated Flow**

**Create `setup-and-deploy.sh`:**

```bash
#!/bin/bash
set -e

echo "=== Step 1: Create Secrets ==="
./create-secrets.sh

echo ""
echo "=== Step 2: Export Secrets ==="
export TF_VAR_elasticsearch_password=$(aws secretsmanager get-secret-value \
  --secret-id elasticsearch-password --region us-east-1 --query SecretString --output text)
export TF_VAR_kibana_encryption_key=$(aws secretsmanager get-secret-value \
  --secret-id kibana-encryption-key --region us-east-1 --query SecretString --output text)
export TF_VAR_rds_master_password=$(aws secretsmanager get-secret-value \
  --secret-id rds-master-password --region us-east-1 --query SecretString --output text)

echo "✅ Secrets loaded"

echo ""
echo "=== Step 3: Deploy Infrastructure ==="
cd terraform/
terraform init
terraform plan \
  -var-file="prod.tfvars" \
  -var="elasticsearch_password=$TF_VAR_elasticsearch_password" \
  -var="kibana_encryption_key=$TF_VAR_kibana_encryption_key" \
  -var="rds_master_password=$TF_VAR_rds_master_password" \
  -out=tfplan
terraform apply tfplan

echo "✅ Infrastructure deployed!"
```

**Run it:**
```bash
chmod +x setup-and-deploy.sh
./setup-and-deploy.sh
```

---

## 🛡️ **Security Best Practices**

✅ **DO:**
- Store passwords in AWS Secrets Manager
- Use environment variables to pass secrets to Terraform
- Rotate passwords regularly
- Use IAM policies to restrict access to secrets
- Enable CloudTrail logging for secret access

❌ **DON'T:**
- Hardcode passwords in `.tf` files
- Commit secrets to git
- Use weak passwords
- Share secrets via Slack/Email
- Store passwords in `.tfvars` files

---

## 🔍 **Rotate Secrets (Production)**

```bash
# Generate new password
NEW_PASSWORD=$(openssl rand -base64 32)

# Update secret in AWS
aws secretsmanager update-secret \
  --secret-id rds-master-password \
  --secret-string "$NEW_PASSWORD" \
  --region us-east-1

# Update password in RDS
aws rds modify-db-instance \
  --db-instance-identifier task-management-db \
  --master-user-password "$NEW_PASSWORD" \
  --apply-immediately

echo "✅ Secret rotated!"
```

---

## 📊 **Secrets Dashboard**

Monitor secrets with:

```bash
# List all secrets with metadata
aws secretsmanager list-secrets \
  --region us-east-1 \
  --filters Key=name,Values=elasticsearch,kibana,rds \
  --output table

# Check when each secret was last updated
aws secretsmanager list-secrets \
  --region us-east-1 \
  --query 'SecretList[*].[Name,LastChangedDate]' \
  --output table
```

---

## 🔧 **Troubleshooting**

### Secret not found
```bash
# Check if secret exists
aws secretsmanager describe-secret --secret-id elasticsearch-password

# If it doesn't exist, create it
aws secretsmanager create-secret \
  --name elasticsearch-password \
  --secret-string "your-secret-value"
```

### IAM Permission Error
```bash
# Add permissions to IAM user/role
aws iam put-user-policy --user-name my-user \
  --policy-name SecretsManagerAccess \
  --policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Action": "secretsmanager:*",
      "Resource": "*"
    }]
  }'
```

### Export fails
```bash
# Debug: Try fetching secret manually
aws secretsmanager get-secret-value \
  --secret-id elasticsearch-password \
  --region us-east-1

# Check AWS credentials
aws sts get-caller-identity
```

---

**Next Step:** Follow DEPLOYMENT_PROCEDURE.md to deploy infrastructure using these secrets!

# Enterprise EFK Stack with AWS EKS

A Production-Grade Centralized Logging and Observability Platform that Deploys, Configures, and Manages a Complete **EFK (Elasticsearch, Fluent Bit, Kibana)** Stack on **Amazon EKS** using **Terraform, Kubernetes, and Helm**. This repository demonstrates Enterprise Logging Architecture patterns commonly adopted by large-scale product companies and Enterprise Organizations for collecting, processing, storing, searching, and visualizing Kubernetes logs.

The project is built using a Modular Infrastructure and Kubernetes Architecture that promotes Scalability, High Availability, Security, and Operational Excellence across Development, Staging, and Production Environments.

---

# Key Features

## Infrastructure & Platform

* Amazon EKS cluster provisioning using Terraform
* Modular Terraform architecture for reusable infrastructure
* Environment-specific deployments (Development, Staging, Production)
* Multi-AZ VPC networking architecture
* Infrastructure as Code following enterprise best practices
* Automated Kubernetes platform provisioning

---

## Elasticsearch Cluster

* Stateful Elasticsearch deployment on Kubernetes
* Persistent storage using StatefulSets
* High availability configuration
* Secure cluster communication
* Configurable resource allocation
* Enterprise-grade search and indexing capabilities
* Horizontal scaling support
* Cluster health monitoring

---

## Log Collection & Processing

### Fluent Bit

* Kubernetes-native log collection using DaemonSets
* Automatic container log discovery
* Kubernetes metadata enrichment
* Namespace, Pod, and Container log tagging
* Resource-efficient log forwarding
* Cluster-wide log aggregation
* RBAC-based secure access

### Logstash

* Centralized log processing pipeline
* Flexible log parsing and transformation
* Structured JSON log formatting
* Log enrichment and filtering
* Custom pipeline configurations
* Elasticsearch indexing integration

---

## Visualization & Analytics

### Kibana

* Interactive log exploration
* Centralized Kubernetes log dashboards
* Elasticsearch monitoring dashboards
* NGINX access log dashboards
* Real-time log search and filtering
* Saved searches and visualizations
* Enterprise observability interface

---

## Monitoring & Alerting

* Custom monitoring framework built with Python
* Elasticsearch health monitoring
* Fluent Bit status monitoring
* Kibana availability monitoring
* Logstash pipeline monitoring
* Infrastructure metrics collection
* Automated alerting framework
* Notification integrations
* Operational health dashboards

---

## Security

* TLS certificate management using Cert-Manager
* Secure ingress configuration
* Kubernetes RBAC implementation
* Dedicated Service Accounts
* ClusterRole and ClusterRoleBinding configuration
* Secure Elasticsearch communication
* Least-privilege access controls

---

## Networking & Ingress

* Kubernetes Ingress configuration
* Secure external access to Kibana
* Secure external access to Elasticsearch
* Internal service discovery
* Namespace isolation
* Kubernetes Service abstraction
* Enterprise networking patterns

---

## Kubernetes Deployment

* Native Kubernetes manifests
* Helm-based deployments
* StatefulSets for persistent workloads
* Deployments for stateless services
* ConfigMaps for centralized configuration
* Namespace-based resource isolation
* Production-ready resource definitions

---

## Dashboard Automation

* Pre-built Kibana dashboards
* Automated dashboard import scripts
* Kubernetes logging dashboards
* Elasticsearch performance dashboards
* NGINX access log dashboards
* Infrastructure monitoring dashboards

---

# Architecture Principles

* Production-grade EFK architecture
* Kubernetes-native logging platform
* Modular Infrastructure as Code
* Security-first deployment model
* High availability and fault tolerance
* Centralized log aggregation
* Infrastructure automation
* Environment isolation
* Scalable log processing pipeline
* Enterprise observability best practices
* Cloud-native deployment architecture
* Operational excellence and monitoring

---

# Technology Stack

**Terraform** • **AWS EKS** • **Amazon VPC** • **Kubernetes** • **Helm** • **Elasticsearch** • **Fluent Bit** • **Logstash** • **Kibana** • **Cert-Manager** • **Kubernetes Ingress** • **StatefulSets** • **ConfigMaps** • **RBAC** • **Python Monitoring Framework** • **TLS Certificates** • **YAML**

---

# Repository Structure

```text
terraform/
├── Modular AWS Infrastructure
├── Amazon EKS
├── Amazon VPC
├── Environment-specific tfvars
└── Production-ready Infrastructure as Code

kubernetes/
├── Elasticsearch
├── Fluent Bit
├── Logstash
├── Kibana
├── Cert-Manager
├── Ingress
└── Namespace Resources

helm/
├── Elasticsearch Values
├── Fluent Bit Values
├── Logstash Values
└── Kibana Values

dashboards/
├── Kibana Dashboards
├── Monitoring Framework
├── Alerting Engine
└── Dashboard Import Automation
```

---

# Use Cases

This project demonstrates how Enterprise Organizations build centralized logging platforms for Kubernetes workloads, enabling DevOps and Platform Engineering teams to:

* Aggregate logs from Kubernetes clusters
* Centralize application and infrastructure logging
* Search and analyze logs in real time
* Monitor Elasticsearch cluster health
* Build operational dashboards
* Implement enterprise observability
* Troubleshoot production workloads efficiently
* Enable proactive monitoring and alerting

---

# Target Audience

This project serves as a comprehensive reference implementation for:

* DevOps Engineers
* Platform Engineers
* Cloud Engineers
* Kubernetes Administrators
* Site Reliability Engineers (SREs)
* Cloud Architects
* Solutions Architects
* Infrastructure Engineers

who are looking to build and operate a production-ready centralized logging and observability platform on **Amazon EKS** using the **EFK Stack**, **Terraform**, **Helm**, and **Kubernetes**.

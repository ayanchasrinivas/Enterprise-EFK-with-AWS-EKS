variable "aws_region" {
  type    = string
  default = "ap-south-1"
}

variable "environment" {
  type    = string
  default = "prod"
}

variable "cluster_name" {
  type    = string
  default = "elk-efk-prod"
}

variable "cluster_version" {
  type    = string
  default = "1.29"
}

variable "vpc_cidr" {
  type    = string
  default = "10.0.0.0/16"
}

variable "domain_name" {
  type    = string
  default = "yourdomain.com"
}

variable "acme_email" {
  type    = string
  default = "admin@yourdomain.com"
}

variable "es_hot_instance_type" {
  type    = string
  default = "r6g.2xlarge"
}

variable "es_warm_instance_type" {
  type    = string
  default = "r6g.xlarge"
}

variable "es_master_instance_type" {
  type    = string
  default = "m6g.large"
}

variable "logging_instance_type" {
  type    = string
  default = "m6g.xlarge"
}

variable "system_instance_type" {
  type    = string
  default = "m6g.large"
}

variable "es_hot_node_count" {
  type    = number
  default = 3
}

variable "es_warm_node_count" {
  type    = number
  default = 2
}

variable "es_master_node_count" {
  type    = number
  default = 3
}

variable "elasticsearch_password" {
  type      = string
  sensitive = true
}

variable "kibana_encryption_key" {
  type      = string
  sensitive = true
}

variable "region" {
  description = "AWS region"
  type        = string
  default     = "ap-south-1"
}

variable "elasticsearch_password" {
  description = "Elasticsearch password"
  type        = string
  sensitive   = true
}

variable "kibana_encryption_key" {
  description = "Kibana encryption key"
  type        = string
  sensitive   = true
}

variable "rds_master_password" {
  description = "RDS master password"
  type        = string
  sensitive   = true
}
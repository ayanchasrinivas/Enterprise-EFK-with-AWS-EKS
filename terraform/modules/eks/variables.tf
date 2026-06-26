variable "cluster_name" {
  type = string
}

variable "cluster_version" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "environment" {
  type = string
}

variable "es_hot_instance_type" {
  type = string
}

variable "es_warm_instance_type" {
  type = string
}

variable "es_master_instance_type" {
  type = string
}

variable "logging_instance_type" {
  type = string
}

variable "system_instance_type" {
  type = string
}

variable "es_hot_node_count" {
  type = number
}

variable "es_warm_node_count" {
  type = number
}

variable "es_master_node_count" {
  type = number
}
module "vpc" {
  source = "./modules/vpc"

  cluster_name = var.cluster_name
  vpc_cidr     = var.vpc_cidr
  environment  = var.environment
}

module "eks" {
  source = "./modules/eks"

  cluster_name            = var.cluster_name
  cluster_version         = var.cluster_version
  vpc_id                  = module.vpc.vpc_id
  private_subnet_ids      = module.vpc.private_subnets
  environment             = var.environment

  es_hot_instance_type    = var.es_hot_instance_type
  es_warm_instance_type   = var.es_warm_instance_type
  es_master_instance_type = var.es_master_instance_type
  logging_instance_type   = var.logging_instance_type
  system_instance_type    = var.system_instance_type

  es_hot_node_count    = var.es_hot_node_count
  es_warm_node_count   = var.es_warm_node_count
  es_master_node_count = var.es_master_node_count
}
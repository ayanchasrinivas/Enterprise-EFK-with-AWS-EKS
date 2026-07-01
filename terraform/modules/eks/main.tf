module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.0"

  cluster_name    = var.cluster_name
  cluster_version = var.cluster_version

  vpc_id                   = var.vpc_id
  subnet_ids               = var.private_subnet_ids
  control_plane_subnet_ids = var.private_subnet_ids

  cluster_endpoint_public_access  = true
  cluster_endpoint_private_access = true

  cluster_addons = {
    coredns = {
      most_recent = true
    }
    kube-proxy = {
      most_recent = true
    }
    vpc-cni = {
      most_recent              = true
      service_account_role_arn = module.vpc_cni_irsa.iam_role_arn
    }
    aws-ebs-csi-driver = {
      most_recent              = true
      service_account_role_arn = module.ebs_csi_irsa.iam_role_arn
    }
  }

  eks_managed_node_groups = {
    system = {
      name           = "system"
      instance_types = [var.system_instance_type]
      min_size       = 2
      max_size       = 4
      desired_size   = 2
      ami_type       = "AL2023_x86_64_STANDARD"

      labels = {
        role       = "system"
        "elk/role" = "system"
      }
    }
  }

  node_security_group_additional_rules = {
    ingress_self_all = {
      description = "Node to node all ports/protocols"
      protocol    = "-1"
      from_port   = 0
      to_port     = 0
      type        = "ingress"
      self        = true
    }
  }
}

module "vpc_cni_irsa" {
  source  = "terraform-aws-modules/iam/aws//modules/iam-role-for-service-accounts-eks"
  version = "~> 5.0"

  role_name_prefix      = "VPC-CNI-IRSA"
  attach_vpc_cni_policy = true
  vpc_cni_enable_ipv4   = true

  oidc_providers = {
    main = {
      provider_arn               = module.eks.oidc_provider_arn
      namespace_service_accounts = ["kube-system:aws-node"]
    }
  }
}

module "ebs_csi_irsa" {
  source  = "terraform-aws-modules/iam/aws//modules/iam-role-for-service-accounts-eks"
  version = "~> 5.0"

  role_name_prefix      = "EBS-CSI-IRSA"
  attach_ebs_csi_policy = true

  oidc_providers = {
    main = {
      provider_arn               = module.eks.oidc_provider_arn
      namespace_service_accounts = ["kube-system:ebs-csi-controller-sa"]
    }
  }
}

resource "kubernetes_storage_class" "elasticsearch_hot" {
  metadata {
    name = "elasticsearch-hot"
  }

  storage_provisioner    = "ebs.csi.aws.com"
  reclaim_policy         = "Retain"
  allow_volume_expansion = true
  volume_binding_mode    = "WaitForFirstConsumer"

  parameters = {
    type       = "gp3"
    encrypted  = "true"
    iops       = "16000"
    throughput = "1000"
    fsType     = "xfs"
  }
}

resource "kubernetes_storage_class" "elasticsearch_warm" {
  metadata {
    name = "elasticsearch-warm"
  }

  storage_provisioner    = "ebs.csi.aws.com"
  reclaim_policy         = "Retain"
  allow_volume_expansion = true
  volume_binding_mode    = "WaitForFirstConsumer"

  parameters = {
    type       = "gp3"
    encrypted  = "true"
    iops       = "3000"
    throughput = "250"
    fsType     = "xfs"
  }
}

output "cluster_name" {
  value = module.eks.cluster_name
}

output "cluster_endpoint" {
  value     = module.eks.cluster_endpoint
  sensitive = true
}

output "cluster_certificate_authority_data" {
  value     = module.eks.cluster_certificate_authority_data
  sensitive = true
}

output "oidc_provider_arn" {
  value = module.eks.oidc_provider_arn
}
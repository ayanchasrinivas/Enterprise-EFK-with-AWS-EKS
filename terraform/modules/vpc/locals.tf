locals {
  azs = slice(data.aws_availability_zones.available.names, 0, 3)

  private_subnets = [
    cidrsubnet(var.vpc_cidr, 4, 0),
    cidrsubnet(var.vpc_cidr, 4, 1),
    cidrsubnet(var.vpc_cidr, 4, 2),
  ]

  public_subnets = [
    cidrsubnet(var.vpc_cidr, 8, 128),
    cidrsubnet(var.vpc_cidr, 8, 129),
    cidrsubnet(var.vpc_cidr, 8, 130),
  ]
}
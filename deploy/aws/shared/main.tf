terraform {
  backend "s3" {}
}

provider "aws" {
  version = "~> 2.0"
  region = "ap-southeast-2"
}

output "weight_tracker_api_ecr_url" {
  value = aws_ecr_repository.weight_tracker_api.repository_url
}

output "weight_tracker_database_migration_ecr_url" {
  value = aws_ecr_repository.weight_tracker_database_migration.repository_url
}

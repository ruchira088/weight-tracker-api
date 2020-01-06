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

output "migration_application_ecr_url" {
  value = aws_ecr_repository.migration_application.repository_url
}

output "email_service_ecr_url" {
  value = aws_ecr_repository.email_service.repository_url
}

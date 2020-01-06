resource "aws_ecr_repository" "weight_tracker_api" {
  name = "weight-tracker-api"
}

resource "aws_ecr_repository" "migration_application" {
  name = "weight-tracker-migration-application"
}

resource "aws_ecr_repository" "email_service" {
  name = "weight-tracker-email-service"
}

resource "aws_ecr_repository" "weight_tracker_api" {
  name = "weight-tracker-api"
}

resource "aws_ecr_repository" "weight_tracker_database_migration" {
  name = "weight-tracker-database-migration"
}

resource "aws_ecr_repository" "weight_tracker_email_service" {
  name = "weight-tracker-email-service"
}

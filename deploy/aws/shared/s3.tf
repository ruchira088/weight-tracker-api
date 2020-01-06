locals {
  s3_bucket = "weight-tracker.ruchij.com"

  images = [ "main-logo.png" ]
}

resource "aws_s3_bucket_object" "images" {
  count = length(local.images)
  bucket = local.s3_bucket
  key = "email/${local.images[count.index]}"
  source = "../../../email-service/assets/${local.images[count.index]}"
  content_type = "image/png"
  acl = "public-read"
}

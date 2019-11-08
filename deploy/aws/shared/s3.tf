locals {
  s3_bucket = "weight-tracker.ruchij.com"

  logos = [ "main-logo.svg", "small-logo.svg" ]
}

resource "aws_s3_bucket_object" "logos" {
  count = length(local.logos)
  bucket = local.s3_bucket
  key = "logos/${local.logos[count.index]}"
  source = "../../../assets/logos/${local.logos[count.index]}"
  content_type = "image/svg+xml"
  acl = "public-read"
}

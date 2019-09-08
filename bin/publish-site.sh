#!/bin/bash
set -euo pipefail
export SBT_GHPAGES_COMMIT_MESSAGE="updated site [ci skip]"

sbt ";set docs/git.remoteRepo := \"https://$GITHUB_TOKEN@github.com/Slakah/uritemplate4s.git\";publishMicrosite"

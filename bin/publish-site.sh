#!/bin/bash
set -ue
export SBT_GHPAGES_COMMIT_MESSAGE="updated site [ci skip]"

exec sbt ";set docs/git.remoteRepo := \"https://$GITHUB_TOKEN@github.com/Slakah/uritemplate4s.git\";publishMicrosite"

#!/bin/bash
set -ue -o pipefail

readonly iv='46E8A1968B174645178FA13AA8F7CFE9'

openssl aes-256-cbc -K "$ENCRYPTED_KEY" -iv $iv -in secring.asc.enc -out secring.asc -d

exec sbt "; clean; +releaseEarly"

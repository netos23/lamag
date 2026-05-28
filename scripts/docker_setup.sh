#!/usr/bin/env bash
set -euo pipefail

# Build the dlama docker image used to run lamac on ARM/Mac hosts.
# See docker/README.MD for context.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "${SCRIPT_DIR}/../docker"

docker build --platform=linux/amd64 -t dlama:1.0.0 .

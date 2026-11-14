#!/bin/bash
# Compatibility wrapper — the installer lives in tools/algogebra-hook/.
set -e
exec "$(cd "$(dirname "$0")" && pwd)/algogebra-hook/install-algogebra-hook.sh" "$@"

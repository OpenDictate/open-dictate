#!/bin/bash
set -euo pipefail
version="${1:-}"
number='(0|[1-9][0-9]*)'
if [[ ! "$version" =~ ^${number}\.${number}\.${number}(-rc\.[1-9][0-9]*)?$ ]]; then
    echo "Version must be X.Y.Z or X.Y.Z-rc.N, without leading zeros." >&2
    exit 1
fi
# Apple bundle short versions allow only the three numeric components.
echo "${version%%-*}"

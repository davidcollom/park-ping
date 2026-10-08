#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
test_dir=$(mktemp -d)
trap 'rm -rf "$test_dir"' EXIT
javac -d "$test_dir" app/src/main/java/uk/co/collom/parkping/AlertEngine.java tests/AlertEngineTest.java
java -cp "$test_dir" uk.co.collom.parkping.AlertEngineTest

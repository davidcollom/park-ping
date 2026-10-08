#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
test_dir=$(mktemp -d)
trap 'rm -rf "$test_dir"' EXIT
javac -d "$test_dir" app/src/main/java/uk/co/collom/parkping/AlertEngine.java app/src/main/java/uk/co/collom/parkping/ProviderThrottle.java tests/AlertEngineTest.java tests/ProviderThrottleTest.java
java -cp "$test_dir" uk.co.collom.parkping.AlertEngineTest
java -cp "$test_dir" uk.co.collom.parkping.ProviderThrottleTest

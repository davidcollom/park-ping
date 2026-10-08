#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
test_dir=$(mktemp -d)
trap 'rm -rf "$test_dir"' EXIT
javac -d "$test_dir" app/src/main/java/uk/co/collom/parkping/AlertEngine.java app/src/main/java/uk/co/collom/parkping/ProviderThrottle.java app/src/main/java/uk/co/collom/parkping/ProviderRequestGate.java tests/AlertEngineTest.java tests/ProviderThrottleTest.java tests/ProviderRequestGateTest.java
java -cp "$test_dir" uk.co.collom.parkping.AlertEngineTest
java -cp "$test_dir" uk.co.collom.parkping.ProviderThrottleTest
java -cp "$test_dir" uk.co.collom.parkping.ProviderRequestGateTest
python3 tests/BrandAssetsTest.py

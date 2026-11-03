#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/core-tests
"${JAVA_HOME:+$JAVA_HOME/bin/}javac" --release 25 -d build/core-tests \
    src/main/java/dev/johnpablo/dungeonsartifacts/core/*.java \
    src/test/java/dev/johnpablo/dungeonsartifacts/core/PlayerLedgerTest.java
"${JAVA_HOME:+$JAVA_HOME/bin/}java" -cp build/core-tests \
    dev.johnpablo.dungeonsartifacts.core.PlayerLedgerTest

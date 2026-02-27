#!/usr/bin/env bash
# Run LRUCache JMH benchmark with profilers (GC + stack). Uses Java 11 and -f 0
# so the benchmark runs in the same JVM as Maven and profilers work.
# Requires: JAVA_HOME for Java 11+ (e.g. sdk use java 11.0.25-tem) or set below.

set -e
cd "$(dirname "$0")"

# Use Java 11+ so benchmark bytecode (target 11) matches runtime
if [ -z "$JAVA_HOME" ] || ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -qE '"1[1-9]|[2-9][0-9]'; then
  for candidate in 11.0.25-tem 17.0.9-tem 11; do
    if [ -d "$HOME/.sdkman/candidates/java/$candidate" ]; then
      export JAVA_HOME="$HOME/.sdkman/candidates/java/$candidate"
      break
    fi
  done
fi

echo "Using JAVA_HOME=$JAVA_HOME"
"$JAVA_HOME/bin/java" -version

# -f 0: no fork (required when using exec:java so forked VM gets correct classpath)
# Profilers run in the same JVM; use for observability, not for pristine throughput numbers.
mvn test-compile exec:java -Dexec.args="-f 0 -wi 5 -i 5 -t 4 -prof gc -prof stack:lines=5 org.jerome.benchmark.LRUCacheBenchmark.readHeavy" "$@"

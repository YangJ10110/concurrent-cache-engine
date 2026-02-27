# LRUCache JMH benchmarks

Separate module so JMH can run **forked** (fresh JVM per fork) for stable results.

## Build

From repo root (builds `cache-engine` then `benchmarks`):

```bash
mvn clean install -DskipTests
```

Or only the benchmarks module (requires `cache-engine` already installed):

```bash
mvn -pl benchmarks package
```

## Run forked benchmarks (recommended)

Use the shaded JAR so forks get a clean classpath:

```bash
# From repo root
java -jar benchmarks/target/benchmarks.jar -f 3 -wi 5 -i 5 -t 4

# Or from benchmarks/
cd benchmarks && java -jar target/benchmarks.jar -f 3 -wi 5 -i 5 -t 4
```

- `-f 3` — 3 forked JVMs (better statistics)
- `-wi 5` — 5 warmup iterations
- `-i 5` — 5 measurement iterations
- `-t 4` — 4 threads

Run a single benchmark:

```bash
java -jar benchmarks/target/benchmarks.jar -f 3 -wi 5 -i 5 -t 4 org.jerome.benchmark.LRUCacheBenchmark.readHeavy
```

## Quick non-forked runs (cache-engine module)

From `cache-engine/` for fast iteration (same JVM, no forks):

```bash
cd cache-engine
mvn test-compile exec:java -Dexec.args="-f 0 -wi 5 -i 5 -t 4 org.jerome.benchmark.LRUCacheBenchmark"
```

Use `-f 0` only for quick checks; use the benchmarks JAR with `-f 3` for reporting numbers.

---

## Phase 5 metrics (ops/sec, p95/p99, lock contention)

These runs map to the metrics and engineering insights in `docs/PHASES.md` (Phase 5).

### 1. Ops/sec + thread scaling (throughput)

Shows whether **O(1) scales linearly under contention** — if doubling threads does not double ops/sec, contention dominates.

```bash
# Default: 4 threads
java -jar benchmarks/target/benchmarks.jar -f 3 -wi 5 -i 5 -t 4

# Thread scaling: run with -t 1, -t 2, -t 4, -t 8 and record Score (ops/s) for each
java -jar benchmarks/target/benchmarks.jar -f 3 -wi 5 -i 5 -t 1
java -jar benchmarks/target/benchmarks.jar -f 3 -wi 5 -i 5 -t 2
java -jar benchmarks/target/benchmarks.jar -f 3 -wi 5 -i 5 -t 4
java -jar benchmarks/target/benchmarks.jar -f 3 -wi 5 -i 5 -t 8
```

### 2. p95 / p99 latency (tail latency)

Reveals **hidden bottlenecks** — high p99 vs median means occasional long waits (locks, GC, etc.).

```bash
# SampleTime mode; output includes p(50.0), p(90.0), p(95.0), p(99.0), etc. in us
java -jar benchmarks/target/benchmarks.jar -f 3 -wi 5 -i 5 -t 4 -bm sampletime -tu us
```

Use `-tu ns` for nanoseconds if you want finer resolution.

### 3. Lock contention (profilers)

Profilers need in-process runs (`-f 0`), so use **cache-engine** with `exec:java`:

```bash
cd cache-engine
# Stack profiler: where threads spend time (e.g. in lock code)
mvn test-compile exec:java -Dexec.args="-f 0 -wi 3 -i 3 -t 4 -prof stack:lines=5 org.jerome.benchmark.LRUCacheBenchmark.readHeavy"
mvn test-compile exec:java -Dexec.args="-f 0 -wi 3 -i 3 -t 4 -prof stack:lines=5 org.jerome.benchmark.LRUCacheBenchmark.writeHeavy"
```

Compare readHeavy vs writeHeavy stacks to see lock hotspots. Optional: `-prof gc` for GC time.

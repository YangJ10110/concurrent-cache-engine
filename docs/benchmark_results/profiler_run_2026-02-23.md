# Profiler run — 2026-02-23

**Benchmark:** `readHeavy`, 4 threads, 5 warmup + 5 measurement iterations (3s each)  
**Mode:** Throughput, **no fork** (`-f 0`) so JMH profilers run in same JVM  
**JVM:** JDK 11.0.25 (Temurin)

---

## Summary

| Metric | Value |
|--------|--------|
| Throughput (avg) | 11.66M ± 3.8M ops/s |
| Throughput (min–max) | 10.9M – 13.0M ops/s |
| GC alloc rate | ~184 MB/s |
| GC alloc norm | ~16.6 B/op |
| GC count (total) | 12 |
| GC time (total) | 10 ms |

**Variance:** High (wide CI). Stack profiler shows lock contention in `LRUCache.get` (ReentrantLock).

---

## Stack profiler (where CPU time goes)

- **RUNNABLE:** 8.7% of samples (rest is WAITING / TIMED_WAITING).
- **Hot in RUNNABLE:**
  - `LRUCacheBenchmark.readHeavy` → `LRUCache.get` (benchmark + cache work).
  - `ReentrantLock.unlock` / `AbstractQueuedSynchronizer.release` (unpark).
  - `HashMap.get` / `HashMap.hash` (map lookup).
  - `AbstractQueuedSynchronizer.acquireQueued` → `ReentrantLock.lock` (lock acquisition in `LRUCache.get`).

So a meaningful fraction of runnable time is in lock acquire/release and map lookup, consistent with lock contention and negative thread scaling.

---

## JFR recording

- **File:** `docs/benchmark_results/recording_readHeavy_4t.jfr`
- **Open with:** JDK Mission Control (JMC) or IntelliJ (Run → Open Flight Recorder Snapshot).
- **Look at:** Lock contention view, thread view (blocked vs runnable), and hot methods.

---

## How to reproduce

From repo root:

```bash
cd cache-engine
export JAVA_HOME=~/.sdkman/candidates/java/11.0.25-tem   # or sdk use java 11.0.25-tem
./run-benchmark-with-profilers.sh
```

Or with JFR (recording to `cache-engine/recording.jfr`):

```bash
MAVEN_OPTS="-XX:StartFlightRecording=filename=recording.jfr,duration=90s,settings=profile" \
  ./run-benchmark-with-profilers.sh
```

# Profilers for LRUCache Benchmark Observability

Use these profilers with your JMH benchmark to understand **high variance** and **lock contention** (e.g. negative thread scaling, 50–60% throughput loss 1T→2T).

All commands assume you run from `cache-engine/` and **Java 11+** (project is compiled for Java 11; Maven must use Java 11+ for the benchmark to run). Example: `export JAVA_HOME=~/.sdkman/candidates/java/11.0.25-tem`

**Quick run with GC + stack profilers:** `./run-benchmark-with-profilers.sh` (uses Java 11 and `-f 0` so profilers work with `exec:java`).

---

## 1. JMH built-in profilers (`-prof`)

No extra install. Pass `-prof <name>` after your benchmark args.

### List available profilers
```bash
mvn test-compile exec:java -Dexec.args="-prof lprof"
```

### GC pressure (variance from allocations/GC)
Use `-f 0` when running with `exec:java` so the forked VM classpath issue is avoided and profilers run in the same JVM.
```bash
JAVA_HOME=~/.sdkman/candidates/java/11.0.25-tem mvn test-compile exec:java -Dexec.args="-f 0 -wi 5 -i 5 -t 4 -prof gc org.jerome.benchmark.LRUCacheBenchmark.readHeavy"
```
Use when: Variance might be from GC pauses or allocation rate.

### Stack sampling (where CPU time goes)
```bash
JAVA_HOME=~/.sdkman/candidates/java/11.0.25-tem mvn test-compile exec:java -Dexec.args="-f 0 -wi 5 -i 5 -t 4 -prof stack:lines=5 org.jerome.benchmark.LRUCacheBenchmark.readHeavy"
```
Use when: You want to see which methods/lines consume CPU (lock vs real work).

### Safepoints (pause variance)
```bash
mvn test-compile exec:java -Dexec.args="-f 1 -wi 3 -i 5 -t 4 -prof safepoints org.jerome.benchmark.LRUCacheBenchmark.readHeavy"
```
Use when: Variance might be from stop-the-world or safepoint-induced pauses.

---

## 2. Java Flight Recorder (JFR) — lock contention & variance

JFR is built into the JDK. Enable it via JVM args so JMH’s process is profiled.

### Record to a file (then open in JMC or IntelliJ)
```bash
MAVEN_OPTS="-XX:StartFlightRecording=filename=recording.jfr,duration=60s" \
  mvn test-compile exec:java \
  -Dexec.args="-f 1 -wi 3 -i 5 -t 4 org.jerome.benchmark.LRUCacheBenchmark.readHeavy"
```
Then open `recording.jfr` in **JDK Mission Control (JMC)** or IntelliJ’s JFR viewer.

**What to look at:**
- **Lock contention** — which locks and for how long
- **Threads** — blocked vs runnable time
- **CPU** — hot methods
- **Allocations** — if GC is contributing to variance

### Minimal overhead settings (for less distortion)
```bash
MAVEN_OPTS="-XX:StartFlightRecording=filename=recording.jfr,duration=60s,settings=profile" \
  mvn test-compile exec:java -Dexec.args="-f 1 -wi 3 -i 5 -t 4 org.jerome.benchmark.LRUCacheBenchmark.readHeavy"
```
Use `settings=default` for more events (more overhead).

---

## 3. Async-profiler — flame graphs & lock contention

Low-overhead sampling profiler; works with JMH’s `-prof async` if the native lib is on the path.

### Install (macOS, example)
```bash
# Clone and build, or download release
# https://github.com/async-profiler/async-profiler
```
Set `ASYNC_PROFILER_HOME` or put the `libasyncProfiler.so` / `.dylib` in a path JMH can find.

### Use with JMH (when available)
```bash
mvn test-compile exec:java -Dexec.args="-f 1 -wi 3 -i 5 -t 4 -prof async:output=flamegraph,simple=true org.jerome.benchmark.LRUCacheBenchmark.readHeavy"
```
Produces flame graphs (HTML) for CPU and can show lock contention.

### Standalone (attach to running JMH process)
If `-prof async` is not set up, run the benchmark, get PID, then:
```bash
# CPU flame graph
./profiler.sh -e cpu -d 30 -f cpu.html <pid>

# Lock contention
./profiler.sh -e lock -d 30 -f lock.html <pid>
```

---

## 4. Reducing variance so profilers see the real bottleneck

- **Longer warmup** — e.g. `-wi 5 -i 5` or longer `-wi 5,3` (5 iterations, 3s each) so JIT and caches stabilize.
- **Single fork** — you already use `@Fork(1)`; multiple forks can add variance (different GC/JIT states).
- **Pin to one benchmark** — e.g. `readHeavy` only, so one workload per run.
- **Fix thread count** — e.g. `-t 4` consistently when comparing runs.
- **GC logging** — add `-Xlog:gc*:file=gc.log` to see if variance correlates with GC.

---

## 5. Quick reference: one run with GC + stack + JFR

```bash
cd cache-engine
MAVEN_OPTS="-XX:StartFlightRecording=filename=recording.jfr,duration=90s,settings=profile" \
  mvn test-compile exec:java \
  -Dexec.args="-f 1 -wi 5 -i 5 -t 4 -prof gc -prof stack:lines=5 org.jerome.benchmark.LRUCacheBenchmark.readHeavy"
```
Then inspect:
- Console: GC stats and stack top.
- `recording.jfr`: lock contention and thread view (open in JMC or IntelliJ).

---

## Summary

| Goal                         | Tool              | Command / approach                                      |
|-----------------------------|-------------------|---------------------------------------------------------|
| GC / allocation variance    | JMH `-prof gc`    | `-prof gc`                                              |
| Where CPU time goes         | JMH `-prof stack` | `-prof stack:lines=5`                                   |
| Pause / safepoint variance  | JMH `-prof safepoints` | `-prof safepoints`                                 |
| Lock contention & threads   | JFR               | `-XX:StartFlightRecording=...` → open in JMC/IntelliJ   |
| Flame graphs + lock view     | Async-profiler    | `-prof async` or standalone `profiler.sh -e lock`        |

For your **high variance** and **negative thread scaling**, start with **JFR** (lock contention + thread view) and **`-prof gc`** (rule out GC); then add **`-prof stack`** or **async-profiler** to see exact hot spots (e.g. `ReentrantLock.lock` vs `LRUCache.get`).

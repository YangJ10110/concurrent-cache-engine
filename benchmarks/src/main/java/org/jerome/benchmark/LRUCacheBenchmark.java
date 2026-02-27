package org.jerome.benchmark;

import org.jerome.LRUCache;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for LRUCache (lives in benchmarks module for proper forked runs).
 *
 * Run forked (recommended):
 *   cd benchmarks && mvn package && java -jar target/benchmarks.jar -f 3 -wi 5 -i 5 -t 4
 *
 * Run from repo root:
 *   mvn -pl benchmarks package && java -jar benchmarks/target/benchmarks.jar -f 3 -wi 5 -i 5 -t 4
 *
 * Thread scaling: add -t 1, -t 2, -t 4, -t 8 and record ops/sec.
 */
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
public class LRUCacheBenchmark {

    private static final int CAPACITY = 10_000;
    /** Key space larger than capacity to trigger evictions in write-heavy workload */
    private static final int KEY_SPACE = 100_000;

    private LRUCache<Integer, String> cache;
    private int[] readKeyIndices;

    @Setup(Level.Trial)
    public void setup() {
        cache = new LRUCache<>(CAPACITY);
        for (int i = 0; i < CAPACITY; i++) {
            cache.put(i, "v" + i);
        }
        readKeyIndices = new int[CAPACITY];
        for (int i = 0; i < CAPACITY; i++) {
            readKeyIndices[i] = i;
        }
    }

    /** Read-heavy: 100% get from existing keys. Use -t 1,2,4,8 for thread scaling. */
    @Benchmark
    @Threads(4)
    public void readHeavy(Blackhole bh) {
        int idx = ThreadLocalRandom.current().nextInt(readKeyIndices.length);
        String v = cache.get(readKeyIndices[idx]);
        bh.consume(v);
    }

    /** Write-heavy: 100% put with keys in [0, KEY_SPACE) to trigger eviction. */
    @Benchmark
    @Threads(4)
    public void writeHeavy(Blackhole bh) {
        int key = ThreadLocalRandom.current().nextInt(KEY_SPACE);
        cache.put(key, "v" + key);
        bh.consume(key);
    }

    /** Mixed: 50% get, 50% put. */
    @Benchmark
    @Threads(4)
    public void mixed(Blackhole bh) {
        if (ThreadLocalRandom.current().nextBoolean()) {
            int idx = ThreadLocalRandom.current().nextInt(readKeyIndices.length);
            String v = cache.get(readKeyIndices[idx]);
            bh.consume(v);
        } else {
            int key = ThreadLocalRandom.current().nextInt(KEY_SPACE);
            cache.put(key, "v" + key);
            bh.consume(key);
        }
    }
}

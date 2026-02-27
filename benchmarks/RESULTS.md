# v0.2.2 benchmark results

**Summary:** Throughput does not scale linearly with threads (contention dominates by 2T). Tail latencies are hundreds of times worse than the median (p99 ~20–24 µs vs p50 ~83 ns), and the stack profiler shows a large share of time in lock acquisition (AQS) and in cache/HashMap code. This matches the Phase 5 expectations: *O(1) algorithms don’t scale linearly under contention* and *tail latency reveals hidden bottlenecks*.

---

### 1. Ops/sec + thread scaling (throughput)  

Shows whether **O(1) scales linearly under contention** — if doubling threads does not double ops/sec, contention dominates.  


### 1T

Benchmark                      Mode  Cnt         Score        Error  Units
LRUCacheBenchmark.mixed       thrpt   15  23571342.545 ± 358910.499  ops/s
LRUCacheBenchmark.readHeavy   thrpt   15  22768054.764 ± 920648.507  ops/s
LRUCacheBenchmark.writeHeavy  thrpt   15  21154614.923 ± 226283.693  ops/s

### 2T

Benchmark                      Mode  Cnt         Score         Error  Units
LRUCacheBenchmark.mixed       thrpt   15  12890476.143 ±  218177.046  ops/s
LRUCacheBenchmark.readHeavy   thrpt   15  24818817.613 ±  136891.947  ops/s
LRUCacheBenchmark.writeHeavy  thrpt   15  11080804.369 ± 1121512.834  ops/s


### 4T

Benchmark                      Mode  Cnt         Score        Error  Units
LRUCacheBenchmark.mixed       thrpt   15  13442098.303 ± 237551.744  ops/s
LRUCacheBenchmark.readHeavy   thrpt   15  12878216.728 ± 793663.742  ops/s
LRUCacheBenchmark.writeHeavy  thrpt   15  11836727.620 ± 453374.266  ops/s

### 8T

Benchmark                      Mode  Cnt         Score        Error  Units
LRUCacheBenchmark.mixed       thrpt   15  13757983.955 ± 438909.200  ops/s
LRUCacheBenchmark.readHeavy   thrpt   15  14716622.357 ± 521639.835  ops/s
LRUCacheBenchmark.writeHeavy  thrpt   15  11944018.330 ± 770468.930  ops/s

**Analysis (throughput & scaling)**  
- **1T baseline:** mixed ~23.6M, readHeavy ~22.8M, writeHeavy ~21.2M ops/s.  
- **2T:** mixed and writeHeavy drop to ~12.9M and ~11.1M — adding a second thread roughly halves throughput instead of doubling it. readHeavy at 2T (~24.8M) is an outlier (measurement or workload quirk).  
- **4T / 8T:** Throughput plateaus: mixed/readHeavy ~13–15M, writeHeavy ~11–12M. Doubling threads from 4 to 8 does not double ops/s.  
- **Interpretation:** If the algorithm scaled linearly, 8T would be near 8× the 1T rate (~160M+). Observed 8T is ~14M (mixed/readHeavy) and ~12M (writeHeavy) — well below 8× and even below 1T. Contention on the shared locks (map + list) dominates; *O(1) per-op cost does not translate to linear scaling under contention*.

---

### 2. p95 / p99 latency (tail latency)  
  
Reveals **hidden bottlenecks** — high p99 vs median means occasional long waits (locks, GC, etc.). 


Benchmark                               Mode      Cnt         Score    Error  Units
LRUCacheBenchmark.mixed               sample  4287273       653.472 ± 44.314  ns/op
LRUCacheBenchmark.mixed:p0.00         sample                    ≈ 0           ns/op
LRUCacheBenchmark.mixed:p0.50         sample                 83.000           ns/op
LRUCacheBenchmark.mixed:p0.90         sample                125.000           ns/op
LRUCacheBenchmark.mixed:p0.95         sample                208.000           ns/op
LRUCacheBenchmark.mixed:p0.99         sample              20992.000           ns/op
LRUCacheBenchmark.mixed:p0.999        sample              43456.000           ns/op
LRUCacheBenchmark.mixed:p0.9999       sample              69248.000           ns/op
LRUCacheBenchmark.mixed:p1.00         sample           10027008.000           ns/op
LRUCacheBenchmark.readHeavy           sample  5323435       710.778 ± 82.605  ns/op
LRUCacheBenchmark.readHeavy:p0.00     sample                    ≈ 0           ns/op
LRUCacheBenchmark.readHeavy:p0.50     sample                 83.000           ns/op
LRUCacheBenchmark.readHeavy:p0.90     sample                292.000           ns/op
LRUCacheBenchmark.readHeavy:p0.95     sample                667.000           ns/op
LRUCacheBenchmark.readHeavy:p0.99     sample              15280.000           ns/op
LRUCacheBenchmark.readHeavy:p0.999    sample              34624.000           ns/op
LRUCacheBenchmark.readHeavy:p0.9999   sample              57856.000           ns/op
LRUCacheBenchmark.readHeavy:p1.00     sample           52166656.000           ns/op
LRUCacheBenchmark.writeHeavy          sample  5389735       665.475 ± 30.391  ns/op
LRUCacheBenchmark.writeHeavy:p0.00    sample                    ≈ 0           ns/op
LRUCacheBenchmark.writeHeavy:p0.50    sample                 83.000           ns/op
LRUCacheBenchmark.writeHeavy:p0.90    sample                125.000           ns/op
LRUCacheBenchmark.writeHeavy:p0.95    sample                209.000           ns/op
LRUCacheBenchmark.writeHeavy:p0.99    sample              23744.000           ns/op
LRUCacheBenchmark.writeHeavy:p0.999   sample              49280.000           ns/op
LRUCacheBenchmark.writeHeavy:p0.9999  sample              72835.379           ns/op
LRUCacheBenchmark.writeHeavy:p1.00    sample           11059200.000           ns/op

**Analysis (tail latency)**  
- **Median (p50):** ~83 ns/op for all three workloads — typical fast path is sub-100 ns.  
- **p95:** 208–667 ns; **p99:** ~15–24 µs (15 280–23 744 ns). So p99 is **~180–280× the median**, and p99.99 / max reach tens of microseconds up to tens of milliseconds.  
- **Interpretation:** The long tail (p99, p99.99, max) indicates that a small fraction of operations occasionally wait much longer — e.g. on locks (mapLock/listLock), GC, or scheduler delays. That is exactly *tail latency revealing hidden bottlenecks*: the median hides the cost of contention and other stalls that show up in the tail.

---

### 3. Lock contention (profilers)  
  
Profilers need in-process runs (`-f 0`), so use **cache-engine** with `exec:java`:  
  
```bash
mvn test-compile exec:java -Dexec.args="-f 0 -wi 3 -i 3 -t 4 -prof stack:lines=5 org.jerome.benchmark.LRUCacheBenchmark.readHeavy"
```
Result "org.jerome.benchmark.LRUCacheBenchmark.readHeavy":
  14987977.558 ±(99.9%) 19944657.133 ops/s [Average]
  (min, avg, max) = (13909846.719, 14987977.558, 16095713.275), stdev = 1093233.958
  CI (99.9%): [≈ 0, 34932634.690] (assumes normal distribution)

Secondary result "org.jerome.benchmark.LRUCacheBenchmark.readHeavy:stack":
Stack profiler:

....[Thread state distributions]....................................................................
 64.4%         WAITING
 23.5%         TIMED_WAITING
 12.1%         RUNNABLE

....[Thread state: WAITING].........................................................................
 47.1%  73.1% jdk.internal.misc.Unsafe.park
              java.util.concurrent.locks.LockSupport.park
              java.util.concurrent.ForkJoinPool.runWorker
              java.util.concurrent.ForkJoinWorkerThread.run

 11.4%  17.7% jdk.internal.misc.Unsafe.park
              java.util.concurrent.locks.LockSupport.park
              java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt
              java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued
              java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire

  5.9%   9.1% jdk.internal.misc.Unsafe.park
              java.util.concurrent.locks.LockSupport.park
              java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await
              java.util.concurrent.ArrayBlockingQueue.take
              org.apache.maven.cli.transfer.SimplexTransferListener.feedConsumer

  0.1%   0.1% jdk.internal.misc.Unsafe.park
              java.util.concurrent.locks.LockSupport.park
              java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await
              java.util.concurrent.CyclicBarrier.dowait
              java.util.concurrent.CyclicBarrier.await

  0.0%   0.0% jdk.internal.misc.Unsafe.park
              java.util.concurrent.locks.LockSupport.park
              java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt
              java.util.concurrent.locks.AbstractQueuedSynchronizer.doAcquireSharedInterruptibly
              java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireSharedInterruptibly

  0.0%   0.0% jdk.internal.misc.Unsafe.park
              java.util.concurrent.locks.LockSupport.park
              java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await
              java.util.concurrent.LinkedBlockingQueue.take
              java.util.concurrent.ThreadPoolExecutor.getTask


....[Thread state: TIMED_WAITING]...................................................................
  5.9%  25.0% java.lang.Object.wait
              java.lang.ref.ReferenceQueue.remove
              jdk.internal.ref.CleanerImpl.run
              java.lang.Thread.run
              jdk.internal.misc.InnocuousThread.run

  5.9%  25.0% jdk.internal.misc.Unsafe.park
              java.util.concurrent.locks.LockSupport.parkNanos
              java.util.concurrent.SynchronousQueue$TransferStack.awaitFulfill
              java.util.concurrent.SynchronousQueue$TransferStack.transfer
              java.util.concurrent.SynchronousQueue.poll

  5.9%  25.0% jdk.internal.misc.Unsafe.park
              java.util.concurrent.locks.LockSupport.parkUntil
              java.util.concurrent.ForkJoinPool.runWorker
              java.util.concurrent.ForkJoinWorkerThread.run

  5.9%  24.9% jdk.internal.misc.Unsafe.park
              java.util.concurrent.locks.LockSupport.parkNanos
              java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos
              java.util.concurrent.LinkedBlockingQueue.poll
              java.util.concurrent.ExecutorCompletionService.poll


....[Thread state: RUNNABLE]........................................................................
  4.5%  37.1% org.jerome.LRUCache.tryDrain
              org.jerome.LRUCache.get
              org.jerome.benchmark.LRUCacheBenchmark.readHeavy
              org.jerome.benchmark.jmh_generated.LRUCacheBenchmark_readHeavy_jmhTest.readHeavy_thrpt_jmhStub
              org.jerome.benchmark.jmh_generated.LRUCacheBenchmark_readHeavy_jmhTest.readHeavy_Throughput

  1.9%  15.6% jdk.internal.misc.Unsafe.unpark
              java.util.concurrent.locks.LockSupport.unpark
              java.util.concurrent.locks.AbstractQueuedSynchronizer.unparkSuccessor
              java.util.concurrent.locks.AbstractQueuedSynchronizer.release
              java.util.concurrent.locks.ReentrantLock.unlock

  1.6%  12.9% java.util.HashMap.hash
              java.util.HashMap.get
              org.jerome.LRUCache.get
              org.jerome.benchmark.LRUCacheBenchmark.readHeavy
              org.jerome.benchmark.jmh_generated.LRUCacheBenchmark_readHeavy_jmhTest.readHeavy_thrpt_jmhStub

  1.4%  11.7% org.jerome.benchmark.LRUCacheBenchmark.readHeavy
              org.jerome.benchmark.jmh_generated.LRUCacheBenchmark_readHeavy_jmhTest.readHeavy_thrpt_jmhStub
              org.jerome.benchmark.jmh_generated.LRUCacheBenchmark_readHeavy_jmhTest.readHeavy_Throughput
              jdk.internal.reflect.NativeMethodAccessorImpl.invoke0
              jdk.internal.reflect.NativeMethodAccessorImpl.invoke

  0.8%   7.0% org.jerome.benchmark.LRUCacheBenchmark.readHeavy
              org.jerome.benchmark.jmh_generated.LRUCacheBenchmark_readHeavy_jmhTest.readHeavy_thrpt_jmhStub
              org.jerome.benchmark.jmh_generated.LRUCacheBenchmark_readHeavy_jmhTest.readHeavy_Throughput
              jdk.internal.reflect.GeneratedMethodAccessor11.invoke
              jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke

  0.8%   6.3% java.util.concurrent.ConcurrentLinkedQueue.offer
              org.jerome.LRUCache.get
              org.jerome.benchmark.LRUCacheBenchmark.readHeavy
              org.jerome.benchmark.jmh_generated.LRUCacheBenchmark_readHeavy_jmhTest.readHeavy_thrpt_jmhStub
              org.jerome.benchmark.jmh_generated.LRUCacheBenchmark_readHeavy_jmhTest.readHeavy_Throughput

  0.5%   4.5% java.util.concurrent.ConcurrentLinkedQueue.poll
              org.jerome.LRUCache.tryDrain
              org.jerome.LRUCache.get
              org.jerome.benchmark.LRUCacheBenchmark.readHeavy
              org.jerome.benchmark.jmh_generated.LRUCacheBenchmark_readHeavy_jmhTest.readHeavy_thrpt_jmhStub

  0.3%   2.5% java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued
              java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire
              java.util.concurrent.locks.ReentrantLock.lock
              org.jerome.LRUCache.get
              org.jerome.benchmark.LRUCacheBenchmark.readHeavy

  0.2%   1.4% jdk.internal.misc.Unsafe.park
              java.util.concurrent.locks.LockSupport.park
              java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt
              java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued
              java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire

  0.1%   0.5% java.util.HashMap.getNode
              java.util.HashMap.get
              org.jerome.LRUCache.get
              org.jerome.benchmark.LRUCacheBenchmark.readHeavy
              org.jerome.benchmark.jmh_generated.LRUCacheBenchmark_readHeavy_jmhTest.readHeavy_thrpt_jmhStub

  0.0%   0.4% <other>

Benchmark                           Mode  Cnt         Score          Error  Units
LRUCacheBenchmark.readHeavy        thrpt    3  14987977.558 ± 19944657.133  ops/s
LRUCacheBenchmark.readHeavy:stack  thrpt                NaN                   ---

```bash
mvn test-compile exec:java -Dexec.args="-f 0 -wi 3 -i 3 -t 4 -prof stack:lines=5 org.jerome.benchmark.LRUCacheBenchmark.writeHeavy"
```

Result "org.jerome.benchmark.LRUCacheBenchmark.writeHeavy":
  11758478.721 ±(99.9%) 5287830.601 ops/s [Average]
  (min, avg, max) = (11445550.801, 11758478.721, 12017736.349), stdev = 289843.838
  CI (99.9%): [6470648.120, 17046309.322] (assumes normal distribution)

Secondary result "org.jerome.benchmark.LRUCacheBenchmark.writeHeavy:stack":
Stack profiler:

....[Thread state distributions]....................................................................
 66.0%         WAITING
 26.6%         TIMED_WAITING
  7.3%         RUNNABLE

....[Thread state: WAITING].........................................................................
 40.0%  60.6% jdk.internal.misc.Unsafe.park
              java.util.concurrent.locks.LockSupport.park
              java.util.concurrent.ForkJoinPool.runWorker
              java.util.concurrent.ForkJoinWorkerThread.run

 19.3%  29.2% jdk.internal.misc.Unsafe.park
              java.util.concurrent.locks.LockSupport.park
              java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt
              java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued
              java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire

  6.7%  10.1% jdk.internal.misc.Unsafe.park
              java.util.concurrent.locks.LockSupport.park
              java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await
              java.util.concurrent.ArrayBlockingQueue.take
              org.apache.maven.cli.transfer.SimplexTransferListener.feedConsumer

  0.0%   0.1% jdk.internal.misc.Unsafe.park
              java.util.concurrent.locks.LockSupport.park
              java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await
              java.util.concurrent.LinkedBlockingQueue.take
              java.util.concurrent.ThreadPoolExecutor.getTask

  0.0%   0.1% jdk.internal.misc.Unsafe.park
              java.util.concurrent.locks.LockSupport.park
              java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await
              java.util.concurrent.CyclicBarrier.dowait
              java.util.concurrent.CyclicBarrier.await

  0.0%   0.0% jdk.internal.misc.Unsafe.park
              java.util.concurrent.locks.LockSupport.park
              java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt
              java.util.concurrent.locks.AbstractQueuedSynchronizer.doAcquireSharedInterruptibly
              java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireSharedInterruptibly


....[Thread state: TIMED_WAITING]...................................................................
  6.7%  25.0% java.lang.Object.wait
              java.lang.ref.ReferenceQueue.remove
              jdk.internal.ref.CleanerImpl.run
              java.lang.Thread.run
              jdk.internal.misc.InnocuousThread.run

  6.7%  25.0% jdk.internal.misc.Unsafe.park
              java.util.concurrent.locks.LockSupport.parkNanos
              java.util.concurrent.SynchronousQueue$TransferStack.awaitFulfill
              java.util.concurrent.SynchronousQueue$TransferStack.transfer
              java.util.concurrent.SynchronousQueue.poll

  6.7%  25.0% jdk.internal.misc.Unsafe.park
              java.util.concurrent.locks.LockSupport.parkUntil
              java.util.concurrent.ForkJoinPool.runWorker
              java.util.concurrent.ForkJoinWorkerThread.run

  6.6%  24.9% jdk.internal.misc.Unsafe.park
              java.util.concurrent.locks.LockSupport.parkNanos
              java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos
              java.util.concurrent.LinkedBlockingQueue.poll
              java.util.concurrent.ExecutorCompletionService.poll


....[Thread state: RUNNABLE]........................................................................
  2.0%  27.9% jdk.internal.misc.Unsafe.unpark
              java.util.concurrent.locks.LockSupport.unpark
              java.util.concurrent.locks.AbstractQueuedSynchronizer.unparkSuccessor
              java.util.concurrent.locks.AbstractQueuedSynchronizer.release
              java.util.concurrent.locks.ReentrantLock.unlock

  2.0%  27.8% java.util.HashMap.hash
              java.util.HashMap.remove
              org.jerome.LRUCache.put
              org.jerome.benchmark.LRUCacheBenchmark.writeHeavy
              org.jerome.benchmark.jmh_generated.LRUCacheBenchmark_writeHeavy_jmhTest.writeHeavy_thrpt_jmhStub

  0.9%  11.8% java.util.HashMap.hash
              java.util.HashMap.put
              org.jerome.LRUCache.put
              org.jerome.benchmark.LRUCacheBenchmark.writeHeavy
              org.jerome.benchmark.jmh_generated.LRUCacheBenchmark_writeHeavy_jmhTest.writeHeavy_thrpt_jmhStub

  0.7%  10.2% java.util.HashMap.putVal
              java.util.HashMap.put
              org.jerome.LRUCache.put
              org.jerome.benchmark.LRUCacheBenchmark.writeHeavy
              org.jerome.benchmark.jmh_generated.LRUCacheBenchmark_writeHeavy_jmhTest.writeHeavy_thrpt_jmhStub

  0.5%   6.5% org.jerome.benchmark.jmh_generated.LRUCacheBenchmark_writeHeavy_jmhTest.writeHeavy_thrpt_jmhStub
              org.jerome.benchmark.jmh_generated.LRUCacheBenchmark_writeHeavy_jmhTest.writeHeavy_Throughput
              jdk.internal.reflect.NativeMethodAccessorImpl.invoke0
              jdk.internal.reflect.NativeMethodAccessorImpl.invoke
              jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke

  0.4%   5.1% java.util.HashMap.remove
              org.jerome.LRUCache.put
              org.jerome.benchmark.LRUCacheBenchmark.writeHeavy
              org.jerome.benchmark.jmh_generated.LRUCacheBenchmark_writeHeavy_jmhTest.writeHeavy_thrpt_jmhStub
              org.jerome.benchmark.jmh_generated.LRUCacheBenchmark_writeHeavy_jmhTest.writeHeavy_Throughput

  0.2%   3.1% java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued
              java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire
              java.util.concurrent.locks.ReentrantLock.lock
              org.jerome.LRUCache.put
              org.jerome.benchmark.LRUCacheBenchmark.writeHeavy

  0.2%   2.8% java.util.HashMap.getNode
              java.util.HashMap.get
              org.jerome.LRUCache.put
              org.jerome.benchmark.LRUCacheBenchmark.writeHeavy
              org.jerome.benchmark.jmh_generated.LRUCacheBenchmark_writeHeavy_jmhTest.writeHeavy_thrpt_jmhStub

  0.2%   2.5% org.jerome.benchmark.jmh_generated.LRUCacheBenchmark_writeHeavy_jmhTest.writeHeavy_thrpt_jmhStub
              org.jerome.benchmark.jmh_generated.LRUCacheBenchmark_writeHeavy_jmhTest.writeHeavy_Throughput
              jdk.internal.reflect.GeneratedMethodAccessor10.invoke
              jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke
              java.lang.reflect.Method.invoke

  0.1%   1.9% jdk.internal.misc.Unsafe.park
              java.util.concurrent.locks.LockSupport.park
              java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt
              java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued
              java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire

  0.0%   0.4% <other>


  Benchmark                            Mode  Cnt         Score         Error  Units
LRUCacheBenchmark.writeHeavy        thrpt    3  11758478.721 ± 5287830.601  ops/s
LRUCacheBenchmark.writeHeavy:stack  thrpt                NaN                  ---

**Analysis (lock contention)**  
- **Thread states:** readHeavy — 64.4% WAITING, 23.5% TIMED_WAITING, 12.1% RUNNABLE. writeHeavy — 66% WAITING, 26.6% TIMED_WAITING, 7.3% RUNNABLE. So only ~7–12% of samples are in RUNNABLE (doing work); the rest are blocked or waiting, consistent with lock contention.  
- **readHeavy RUNNABLE:** `LRUCache.tryDrain`/get, `ReentrantLock.unlock`, `HashMap.get`, and `AbstractQueuedSynchronizer.acquireQueued` (acquiring the lock) show up. So both lock acquisition and the cache/HashMap path are on the hot path.  
- **writeHeavy RUNNABLE:** A larger share is in `ReentrantLock.unlock`, `HashMap.hash`/remove/put, and `AbstractQueuedSynchronizer.acquireQueued`; writeHeavy has *less* RUNNABLE (7.3% vs 12.1%) and a *higher* share of WAITING in AQS (19.3% vs 11.4% for readHeavy). So the write path spends more time waiting on locks and less time doing useful work.  
- **Interpretation:** The stack profiler confirms that lock acquisition (AQS) and the cache (HashMap, LRUCache.get/put) dominate. Write-heavy workload contends more on the same locks, which matches the lower throughput and higher tail latency for writes. Reducing contention (e.g. split locks, read-write locks, or per-segment locking) is the natural next step (Phase 6).

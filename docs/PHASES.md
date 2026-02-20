# Concurrent Cache Engine — Project Phases

## Version Mapping

| Version | Phase | Description |
|---------|-------|-------------|
| v0.1.0 | 1-2 | DoublyLinkedList foundation |
| v0.2.0 | 1-4 | Core LRU with concurrency and validation |
| v0.3.0 | 5 | JMH benchmarks (planned) |
| v0.4.0 | 3 | LFU extension (planned) |
| v1.0.0 | 8 | Production-ready (planned) |

---

## Phase 0 — Problem Framing & Constraints

**Status:** ✅ Completed

### Goal

Define what "production-grade cache" means in practice.
Make explicit trade-offs instead of defaulting to textbook correctness.

### Key Decisions

- Java 11 (predictable JVM + concurrency primitives)
- In-memory cache library (not service-bound)
- Multi-threaded access as a first-class constraint
- Eventual consistency acceptable

### Output

- Written design document
- Explicit invariants
- Lock ordering rules
- Clear non-goals (e.g., no persistence)

---

## Phase 1 — Core Data Structures (LRU Foundation)

**Status:** ✅ Completed  
**Version:** v0.1.0

### Goal

Implement cache primitives manually to understand invariants and memory behavior.

### Implemented

- `Node<K,V>` — minimal data container
- `DoublyLinkedList<K,V>` — sentinel-based LRU ordering
- Hand-managed attach / detach semantics

### Why This Matters

- Full control over memory layout
- Clear ownership of invariants
- No hidden behavior from standard library collections

### Engineering Insight

- Correctness in pointer manipulation is harder than it looks
- Sentinels dramatically simplify boundary conditions

---

## Phase 2 — Thread Safety & Concurrency

**Status:** ✅ Completed  
**Version:** v0.1.0

### Goal

Support safe concurrent access under contention.
Understand real-world trade-offs between correctness and throughput.

### Implemented

- Split locking (`mapLock`, `listLock`)
- Strict lock acquisition order
- Relaxed LRU updates on reads
- Strict recency updates on writes and eviction

### Key Trade-offs

- Throughput > perfect recency
- Reads optimized for contention
- Eventual consistency acceptable

### Engineering Insight

- Lock scope matters more than lock type
- Relaxing invariants can drastically improve scalability

---

## Phase 3 — LFU Policy Extension

**Status:** 🔜 Planned  
**Target Version:** v0.4.0

### Goal

Extend eviction strategy to LFU to explore algorithmic and structural trade-offs.

### Planned

- Frequency tracking
- Eviction based on usage counts
- Heap / frequency structure integration (implementation-dependent)

### Notes

- Design document mentions "pluggable interface" for eviction policies — implementation pending
- LFU adds algorithmic complexity and metadata overhead
- Maintaining accurate frequencies under concurrency is harder than LRU

### Engineering Insight (Expected)

- LFU correctness is more expensive than LRU correctness

---

## Phase 4 — Correctness Validation

**Status:** ✅ Completed  
**Version:** v0.2.0

### Goal

Prove correctness before optimizing further.

### Progress

- [x] Single-threaded unit tests (partial — 9 DLL tests, 4 cache tests)
- [x] Multi-threaded stress tests (basic — 2 concurrent tests)
- [x] Invariant checks under concurrent access
- [x] Failure injection (forced evictions, contention spikes)
- [x] Edge case coverage (capacity=1, null handling)
- [x] Verification of relaxed LRU skip behavior

### Why This Matters

- Concurrency bugs hide under low load
- Correctness must be established before performance claims

---

## Phase 5 — Performance Measurement & Contention Analysis

**Status:** 🔜 Next  
**Target Version:** v0.3.0

### Goal

Observe real system behavior under load.

### Planned

- JMH benchmarks
- Read-heavy vs write-heavy workloads
- Thread scaling experiments
- Throughput and latency distribution analysis

### Metrics

- Ops/sec
- p95 / p99 latency
- Lock contention time

### Engineering Insight (Expected)

- O(1) algorithms don't scale linearly under contention
- Tail latency reveals hidden bottlenecks

---

## Phase 6 — Comparative Design Experiments

**Status:** 🔜 Planned

### Goal

Learn by comparison, not speculation.

### Experiments

- Coarse-grained lock vs split locks
- Strict LRU vs relaxed LRU
- LRU vs LFU under identical workloads
- Read/write ratio sensitivity

### Output

- Graphs
- Written conclusions
- Explicit "why this design wins/loses" notes

---

## Phase 7 — TTL & Time-Based Eviction

**Status:** 🔜 Planned

### Goal

Introduce time as a new eviction dimension.

### Challenges

- Efficient expiry tracking
- Background cleanup vs lazy eviction
- Interaction with LRU/LFU

### Engineering Insight (Expected)

- Time-based eviction introduces scheduler complexity
- Cleanup strategy affects latency predictability

---

## Phase 8 — Production Hardening

**Status:** 🔜 Optional / Stretch  
**Target Version:** v1.0.0

### Goal

Push toward real-world readiness.

### Possible Additions

- Metrics hooks
- Cache size introspection
- Hit/miss counters
- Safe shutdown semantics

### Non-goal

- Distributed cache
- Persistence layer

---

## Phase 9 — Documentation & Knowledge Capture

**Status:** 🔄 Ongoing

### Goal

Make reasoning visible.

### Deliverables

- Design document
- Trade-off tables
- Benchmark results
- README focused on engineering insight

### Why This Matters

- Seniors are judged on reasoning, not code volume
- Recruiters look for how you think, not just what you built
